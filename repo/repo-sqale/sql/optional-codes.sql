-- @formatter:off
-- Describes c_object.objectClassType
CREATE TABLE c_objtype (
    id INT PRIMARY KEY,
    name VARCHAR2(64)
);

-- Based on RObjectType
INSERT INTO c_objtype VALUES (0, 'CONNECTOR');
INSERT INTO c_objtype VALUES (1, 'CONNECTOR_HOST');
INSERT INTO c_objtype VALUES (2, 'GENERIC_OBJECT');
INSERT INTO c_objtype VALUES (3, 'OBJECT');
INSERT INTO c_objtype VALUES (4, 'VALUE_POLICY');
INSERT INTO c_objtype VALUES (5, 'RESOURCE');
INSERT INTO c_objtype VALUES (6, 'SHADOW');
INSERT INTO c_objtype VALUES (7, 'ROLE');
INSERT INTO c_objtype VALUES (8, 'SYSTEM_CONFIGURATION');
INSERT INTO c_objtype VALUES (9, 'TASK');
INSERT INTO c_objtype VALUES (10, 'USER');
INSERT INTO c_objtype VALUES (11, 'REPORT');
INSERT INTO c_objtype VALUES (12, 'REPORT_DATA');
INSERT INTO c_objtype VALUES (13, 'OBJECT_TEMPLATE');
INSERT INTO c_objtype VALUES (14, 'NODE');
INSERT INTO c_objtype VALUES (15, 'ORG');
INSERT INTO c_objtype VALUES (16, 'ABSTRACT_ROLE');
INSERT INTO c_objtype VALUES (17, 'FOCUS');
INSERT INTO c_objtype VALUES (18, 'ASSIGNMENT_HOLDER');
INSERT INTO c_objtype VALUES (19, 'SECURITY_POLICY');
INSERT INTO c_objtype VALUES (20, 'LOOKUP_TABLE');
INSERT INTO c_objtype VALUES (21, 'ACCESS_CERTIFICATION_DEFINITION');
INSERT INTO c_objtype VALUES (22, 'ACCESS_CERTIFICATION_CAMPAIGN');
INSERT INTO c_objtype VALUES (23, 'SEQUENCE');
INSERT INTO c_objtype VALUES (24, 'SERVICE');
INSERT INTO c_objtype VALUES (25, 'FORM');
INSERT INTO c_objtype VALUES (26, 'CASE');
INSERT INTO c_objtype VALUES (27, 'FUNCTION_LIBRARY');
INSERT INTO c_objtype VALUES (28, 'OBJECT_COLLECTION');
INSERT INTO c_objtype VALUES (29, 'ARCHETYPE');
INSERT INTO c_objtype VALUES (30, 'DASHBOARD');

-- Describes c_reference.referenceType
CREATE TABLE c_reftype (
    id INT PRIMARY KEY,
    name VARCHAR2(64)
);

-- Based on RReferenceType
INSERT INTO c_reftype
VALUES (0, 'OBJECT_PARENT_ORG');
INSERT INTO c_reftype
VALUES (1, 'USER_ACCOUNT');
INSERT INTO c_reftype
VALUES (2, 'RESOURCE_BUSINESS_CONFIGURATION_APPROVER');
INSERT INTO c_reftype
VALUES (3, '(DEPRECATED) ROLE_APPROVER');
INSERT INTO c_reftype
VALUES (4, '(DEPRECATED) SYSTEM_CONFIGURATION_ORG_ROOT');
INSERT INTO c_reftype
VALUES (5, 'CREATE_APPROVER');
INSERT INTO c_reftype
VALUES (6, 'MODIFY_APPROVER');
INSERT INTO c_reftype
VALUES (7, 'INCLUDE');
INSERT INTO c_reftype
VALUES (8, 'ROLE_MEMBER');
INSERT INTO c_reftype
VALUES (9, 'DELEGATED');
INSERT INTO c_reftype
VALUES (10, 'PERSONA');
INSERT INTO c_reftype
VALUES (11, 'ARCHETYPE');
