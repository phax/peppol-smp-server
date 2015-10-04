-- internal script to reset the complete DB
-- Compare to the DB backup, this is based on the EclipseLink generated code
ALTER TABLE smp_endpoint DROP FOREIGN KEY FK_smp_endpoint_documentIdentifierScheme;
ALTER TABLE smp_ownership DROP FOREIGN KEY FK_smp_ownership_username;
ALTER TABLE smp_ownership DROP FOREIGN KEY FK_smp_ownership_businessIdentifier;
ALTER TABLE smp_process DROP FOREIGN KEY FK_smp_process_documentIdentifierScheme;
ALTER TABLE smp_service_metadata DROP FOREIGN KEY FK_smp_service_metadata_businessIdentifier;
DROP TABLE IF EXISTS smp_endpoint;
DROP TABLE IF EXISTS smp_ownership;
DROP TABLE IF EXISTS smp_process;
DROP TABLE IF EXISTS smp_service_group;
DROP TABLE IF EXISTS smp_service_metadata;
DROP TABLE IF EXISTS smp_user;
DROP TABLE IF EXISTS smp_service_metadata_redirection;
CREATE TABLE smp_endpoint (certificate LONGTEXT NOT NULL, endpointReference VARCHAR(256) NOT NULL, extension LONGTEXT, minimumAuthenticationLevel VARCHAR(256), requireBusinessLevelSignature TINYINT(1) default 0 NOT NULL, serviceActivationDate DATETIME, serviceDescription LONGTEXT NOT NULL, serviceExpirationDate DATETIME, technicalContactUrl VARCHAR(256) NOT NULL, technicalInformationUrl VARCHAR(256), documentIdentifierScheme VARCHAR(25) NOT NULL, processIdentifier VARCHAR(200) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, processIdentifierType VARCHAR(25) NOT NULL, transportProfile VARCHAR(256) NOT NULL, PRIMARY KEY (documentIdentifierScheme, processIdentifier, businessIdentifier, businessIdentifierScheme, documentIdentifier, processIdentifierType, transportProfile));
CREATE TABLE smp_ownership (username VARCHAR(256) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, PRIMARY KEY (username, businessIdentifier, businessIdentifierScheme));
CREATE TABLE smp_process (extension LONGTEXT, documentIdentifierScheme VARCHAR(25) NOT NULL, processIdentifier VARCHAR(200) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, processIdentifierType VARCHAR(25) NOT NULL, PRIMARY KEY (documentIdentifierScheme, processIdentifier, businessIdentifier, businessIdentifierScheme, documentIdentifier, processIdentifierType));
CREATE TABLE smp_service_group (extension LONGTEXT, businessIdentifier VARCHAR(50) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, PRIMARY KEY (businessIdentifier, businessIdentifierScheme));
CREATE TABLE smp_service_metadata (extension LONGTEXT, documentIdentifierScheme VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, PRIMARY KEY (documentIdentifierScheme, businessIdentifier, businessIdentifierScheme, documentIdentifier));
CREATE TABLE smp_user (username VARCHAR(256) NOT NULL UNIQUE, password VARCHAR(256) NOT NULL, PRIMARY KEY (username));
CREATE TABLE smp_service_metadata_redirection (certificateUID VARCHAR(256) NOT NULL, extension LONGTEXT, redirectionUrl VARCHAR(256) NOT NULL, documentIdentifierScheme VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, PRIMARY KEY (documentIdentifierScheme, businessIdentifier, businessIdentifierScheme, documentIdentifier));
ALTER TABLE smp_endpoint ADD CONSTRAINT FK_smp_endpoint_documentIdentifierScheme FOREIGN KEY (documentIdentifierScheme, processIdentifier, businessIdentifier, businessIdentifierScheme, documentIdentifier, processIdentifierType) REFERENCES smp_process (documentIdentifierScheme, processIdentifier, businessIdentifier, businessIdentifierScheme, documentIdentifier, processIdentifierType) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE smp_ownership ADD CONSTRAINT FK_smp_ownership_username FOREIGN KEY (username) REFERENCES smp_user (username) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE smp_ownership ADD CONSTRAINT FK_smp_ownership_businessIdentifier FOREIGN KEY (businessIdentifier, businessIdentifierScheme) REFERENCES smp_service_group (businessIdentifier, businessIdentifierScheme) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE smp_process ADD CONSTRAINT FK_smp_process_documentIdentifierScheme FOREIGN KEY (documentIdentifierScheme, businessIdentifier, businessIdentifierScheme, documentIdentifier) REFERENCES smp_service_metadata (documentIdentifierScheme, businessIdentifier, businessIdentifierScheme, documentIdentifier) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE smp_service_metadata ADD CONSTRAINT FK_smp_service_metadata_businessIdentifier FOREIGN KEY (businessIdentifier, businessIdentifierScheme) REFERENCES smp_service_group (businessIdentifier, businessIdentifierScheme) ON DELETE CASCADE ON UPDATE CASCADE;
INSERT INTO `smp_user` VALUES ('peppol_user','Test1234');
