-- Ensure that user 4 has no role association records in the ConsumerRole table
-- (no data needs to be inserted, or explicitly delete possible interfering data)
DELETE FROM "ConsumerRole" WHERE "ConsumerId" = 4;