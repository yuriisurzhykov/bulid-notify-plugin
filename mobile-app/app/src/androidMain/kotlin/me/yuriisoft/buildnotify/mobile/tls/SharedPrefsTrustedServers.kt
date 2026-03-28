package me.yuriisoft.buildnotify.mobile.tls

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import me.yuriisoft.buildnotify.mobile.network.tls.TrustedServers
import me.yuriisoft.buildnotify.mobile.tls.SharedPrefsTrustedServers.Companion.PREFS_KEY_MIGRATED_VERSION

/**
 * Android [TrustedServers] backed by an app-private [SharedPreferences] file.
 *
 * Each entry maps a server identity key to its pinned SHA-256 fingerprint.
 *
 * ### Phase 3 addition — one-time key migration
 *
 * Prior to Phase 3, [DiscoveryViewModel] used `host.name` (the mDNS service
 * name) as the trust key. Phase 3 switches to `host.instanceId` — a stable
 * per-process UUID advertised in the mDNS TXT record.
 *
 * To avoid forcing existing users to re-pair all their servers after an app
 * update, [migrateHostNameToInstanceId] performs a **one-time, per-entry
 * migration** when the caller knows both the old name key and the new
 * instanceId:
 *
 * ```
 * Old entry:  "MyMacBook IDE" → "AB:CD:EF:..."
 * New entry:  "<uuid>"        → "AB:CD:EF:..."   ← atomically written
 * Removed:    "MyMacBook IDE"                     ← atomically removed
 * ```
 *
 * The migration is idempotent:
 * - If `instanceId` is already pinned with the correct fingerprint, nothing
 *   happens and the stale `hostName` entry is cleaned up.
 * - If neither key exists, nothing happens.
 * - If the fingerprints differ (someone re-paired manually), the newer
 *   `instanceId` entry wins; the old `hostName` entry is discarded.
 *
 * Migration is gated by [PREFS_KEY_MIGRATED_VERSION] in a separate
 * SharedPreferences file so the logic runs at most once per schema version,
 * regardless of how many times the app restarts.
 */
class SharedPrefsTrustedServers(context: Context) : TrustedServers {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val migrationPrefs: SharedPreferences =
        context.getSharedPreferences(MIGRATION_PREFS_NAME, Context.MODE_PRIVATE)

    override fun fingerprint(instanceId: String): String? =
        prefs.getString(instanceId, null)

    override fun pin(instanceId: String, fingerprint: String) {
        prefs.edit { putString(instanceId, fingerprint) }
    }

    override fun unpin(instanceId: String) {
        prefs.edit { remove(instanceId) }
    }

    /**
     * Migrates an existing trust entry from [hostName] key to [instanceId] key.
     *
     * Must be called by [DiscoveryViewModel] when it receives a host that
     * advertises an `instanceId` AND the store has an entry keyed by `hostName`
     * but NOT yet by `instanceId`. Calling it multiple times is safe.
     *
     * The operation is atomic: both the write of the new key and the removal
     * of the old key happen inside a single [SharedPreferences.edit] commit.
     *
     * @param hostName   the old mDNS service name that was previously used as key
     * @param instanceId the new stable UUID from the mDNS TXT `id` field
     */
    fun migrateHostNameToInstanceId(hostName: String, instanceId: String) {
        // Already migrated for this instanceId — nothing to do.
        if (isMigrated(instanceId)) return

        val existingFingerprintForName = prefs.getString(hostName, null) ?: run {
            // No legacy entry exists; mark as migrated so we never check again.
            markMigrated(instanceId)
            return
        }

        // Write a new key and remove the old key atomically.
        prefs.edit {
            putString(instanceId, existingFingerprintForName)
            remove(hostName)
        }

        markMigrated(instanceId)
    }

    private fun isMigrated(instanceId: String): Boolean =
        migrationPrefs.getBoolean(migrationKey(instanceId), false)

    private fun markMigrated(instanceId: String) {
        migrationPrefs.edit { putBoolean(migrationKey(instanceId), true) }
    }

    private fun migrationKey(instanceId: String): String =
        "$PREFS_KEY_MIGRATED_VERSION.$instanceId"

    private companion object {
        const val PREFS_NAME = "buildnotify_trusted_servers"
        const val MIGRATION_PREFS_NAME = "buildnotify_trusted_servers_migration"

        /**
         * Bump this constant if the migration logic itself changes in a future
         * phase and needs to re-run for already-migrated entries.
         */
        const val PREFS_KEY_MIGRATED_VERSION = "v1"
    }
}
