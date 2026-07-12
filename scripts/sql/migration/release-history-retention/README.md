# Release history retention migration

These manual MySQL scripts repair data left by the old soft-delete retention behavior and then
physically purge it. They must be run against `ApolloConfigDB`; they are intentionally outside
`scripts/sql/src` and are not executed as part of an automatic Apollo schema upgrade.

The repair and purge are deliberately limited to rows belonging to the current active `Namespace`
incarnation. The scripts use the millisecond-resolution `DeletedAt` values as the incarnation
boundary: namespace deletion soft-deletes releases and release histories before soft-deleting the
`Namespace` row, so rows whose `DeletedAt` is not later than a matching deleted `Namespace` are
excluded. This remains unambiguous when deletion and recreation happen within the same second. The
precheck reports excluded rows separately; the write scripts never modify them.

Before running a write script, back up the database, stop Apollo Admin Service instances that can
publish or clean releases, and confirm that replication and binary-log capacity can handle the
operation.

Run the scripts in this order:

1. Run `01-precheck-releases-needing-restore.sql`. `ActiveReleaseKeyConflicts` must be `0` before
   continuing; investigate the sample rows if it is not.
2. Run `02-precheck-soft-deleted-rows.sql` and record the retention-scoped and excluded row counts.
3. Run `03-restore-referenced-releases.sql` repeatedly until
   `RemainingReleasesNeedingRestore` is `0`.
4. Run the first precheck again. `ReleasesNeedingRestore` must now be `0`.
5. Run `04-purge-soft-deleted-rows.sql` repeatedly until both
   `RemainingRetentionReleaseHistoryRows` and `RemainingRetentionReleaseRows` are `0`. Excluded
   soft-deleted rows may remain and must not be removed by this migration.
6. Run both prechecks again and restart the stopped services.

The write scripts use batches of 1000 rows to limit transaction size. They are idempotent and may
be stopped between batches. Do not run the purge script before the restore step.
