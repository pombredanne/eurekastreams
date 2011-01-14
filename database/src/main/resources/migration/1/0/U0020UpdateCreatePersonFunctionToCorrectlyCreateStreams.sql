insert into db_version (major, minor, patch, scriptname, description) values (1, 0, '0020', 'U0020UpdateCreatePersonFunctionToCorrectlyCreateStreams.sql', 'Make CreatePerson create the stream properly.');

--Automatic drop and then create to ensure successful creation.
DROP FUNCTION IF EXISTS CreatePerson (_accountid VARCHAR, _firstname VARCHAR, _middlename VARCHAR,  _lastname VARCHAR, _email VARCHAR, _opensocialid VARCHAR);

CREATE FUNCTION CreatePerson (_accountid VARCHAR, _firstname VARCHAR, _middlename VARCHAR,  _lastname VARCHAR, _email VARCHAR, _opensocialid VARCHAR) RETURNS VOID AS
$$
DECLARE
    _startPageTabGroupId BIGINT;
    _profileTabGroupId BIGINT;
    _tabTemplateId BIGINT;
    _streamScopeId BIGINT;
    _personId BIGINT;
    rec RECORD;
BEGIN
    -- create tab for start page
    FOR rec IN INSERT INTO tabgroup (version) values (0) RETURNING id LOOP
        _startPageTabGroupId := rec.id;
    END LOOP;
    FOR rec IN INSERT INTO tabtemplate (version, deleted, tablayout, tabname) VALUES (0, false, 'THREECOLUMN', 'Welcome') RETURNING id LOOP
        _tabTemplateId := rec.id;
    END LOOP;
    INSERT INTO tab (version, deleted, tabindex, tabgroupid, templateid) VALUES (0, false, 0, _startPageTabGroupId, _tabTemplateId);
    
    -- create stream
    FOR rec IN INSERT INTO streamscope (version, scopetype, uniquekey) values (0, 'PERSON', _accountid) RETURNING id LOOP
        _streamScopeId := rec.id;
    END LOOP;
    
    -- create person
    FOR rec IN INSERT INTO person (version, accountid, dateadded, email, firstname, followerscount, followingcount, groupscount, 
            lastname, middlename, opensocialid, preferredname, parentorganizationid, starttabgroupid, 
            updatescount, streamviewhiddenlineindex, streamscopeid, 
            commentable, streampostable, accountlocked)
        VALUES (0, _accountid, now(), _email, _firstname, 0, 0, 0, _lastname, _middlename, _opensocialid, _firstname, 1, 
            _startPageTabGroupId, 0, 3, _streamScopeId, true, true, false) RETURNING id LOOP
        _personId := rec.id;
    END LOOP;

    -- update the stream table with the correct destinationentityid for the newly created user.
    UPDATE streamscope set destinationentityid = _personId where id = _streamScopeId;
    
    -- add views for activity page
    INSERT INTO person_stream (personid, streamid, streamindex) 
    VALUES (_personId, 1, 0), (_personId, 2, 1), (_personId, 3, 2), (_personId, 4, 3);
    
    -- follow self
    INSERT INTO follower (followerid, followingid) VALUES (_personId, _personId);
END;
$$ LANGUAGE plpgsql;