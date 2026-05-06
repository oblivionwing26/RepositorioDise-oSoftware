IF OBJECT_ID('dbo.users', 'U') IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.users')
      AND name = 'ux_users_email'
)
BEGIN
    CREATE UNIQUE INDEX ux_users_email ON dbo.users(email) WHERE email IS NOT NULL
END;
