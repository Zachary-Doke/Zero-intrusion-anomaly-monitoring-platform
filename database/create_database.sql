-- Optional bootstrap script for creating the application database itself.
-- Run this in a privileged connection (for example, connected to `postgres` DB).

CREATE DATABASE platformdb
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;

