/*
 * Copyright (c) 2010 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.persistence.mappers.cache;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.domain.stream.StreamEntityDTO;
import org.eurekastreams.server.persistence.mappers.MapperTest;
import org.eurekastreams.server.persistence.mappers.stream.GetFollowerIds;
import org.eurekastreams.server.persistence.mappers.stream.GetPeopleByAccountIds;
import org.eurekastreams.server.search.modelview.DomainGroupModelView;
import org.eurekastreams.server.search.modelview.PersonModelView;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for the {@link GetCompositeStreamIdsByAssociatedActivity} class.
 * 
 */
public class GetCompositeStreamIdsByAssociatedActivityTest extends MapperTest
{
    /**
     * System under test.
     */
    private GetCompositeStreamIdsByAssociatedActivity sut;

    /**
     * Context for building mock objects.
     */
    private final Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /**
     * Mocked instance of the cache object.
     */
    private final Cache cacheMock = context.mock(Cache.class);

    /**
     * Mapper to get followers of a person.
     */
    private GetFollowerIds personFollowersMapperMock = context.mock(GetFollowerIds.class);

    /**
     * Mapper to get people by account ids.
     */
    private GetPeopleByAccountIds bulkPeopleByAccountIdMapperMock = context.mock(GetPeopleByAccountIds.class);

    /**
     * Test person id for fordp.
     */
    private static final Long TEST_PERSON_ID = 42L;

    /**
     * Prepare the system under test.
     */
    @Before
    public void setup()
    {
        sut = new GetCompositeStreamIdsByAssociatedActivity(personFollowersMapperMock, bulkPeopleByAccountIdMapperMock);
        sut.setCache(cacheMock);
        sut.setEntityManager(getEntityManager());
    }

    /**
     * Test retrieving followers with entity type of Person for the Destination Stream.
     */
    @Test
    public void testGetFollowersWithPersonDestinationStream()
    {
        ActivityDTO testActivity = new ActivityDTO();
        testActivity.setId(1L);

        StreamEntityDTO testDestinationStream = new StreamEntityDTO();
        testDestinationStream.setId(1L);
        testDestinationStream.setUniqueIdentifier("fordp");
        testDestinationStream.setType(EntityType.PERSON);
        testActivity.setDestinationStream(testDestinationStream);

        PersonModelView testPersonModelView = new PersonModelView();
        testPersonModelView.setParentOrganizationShortName("orgShortName");
        testPersonModelView.setEntityId(TEST_PERSON_ID);

        final List<PersonModelView> testPersonModelViewList = new ArrayList<PersonModelView>();
        testPersonModelViewList.add(testPersonModelView);

        final List<Long> followerIds = new ArrayList<Long>();
        followerIds.add(TEST_PERSON_ID);

        context.checking(new Expectations()
        {
            {
                oneOf(bulkPeopleByAccountIdMapperMock).execute(with(any(List.class)));
                will(returnValue(testPersonModelViewList));

                oneOf(personFollowersMapperMock).execute(with(any(Long.class)));
                will(returnValue(followerIds));
            }
        });

        List<Long> results = sut.getFollowers(testActivity);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(TEST_PERSON_ID, results.get(0));
        context.assertIsSatisfied();
    }

    /**
     * Test retrieving followers with entity type of Group for the Destination Stream.
     */
    @Test
    public void testGetFollowersWithGroupDestinationStream()
    {
        ActivityDTO testActivity = new ActivityDTO();
        testActivity.setId(1L);

        StreamEntityDTO testDestinationStream = new StreamEntityDTO();
        testDestinationStream.setId(1L);
        testDestinationStream.setUniqueIdentifier("group1");
        testDestinationStream.setType(EntityType.GROUP);
        testActivity.setDestinationStream(testDestinationStream);

        DomainGroupModelView testGroupModelView = new DomainGroupModelView();
        testGroupModelView.setParentOrganizationShortName("orgShortName");
        testGroupModelView.setEntityId(5L);

        final List<DomainGroupModelView> testGroupModelViewList = new ArrayList<DomainGroupModelView>();
        testGroupModelViewList.add(testGroupModelView);

        List<Long> results = sut.getFollowers(testActivity);
        Assert.assertEquals(0, results.size());
        context.assertIsSatisfied();
    }

    /**
     * Test retrieving followers with an unsupported entity type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetFollowersUnsupportedDestinationStream()
    {
        ActivityDTO testActivity = new ActivityDTO();
        testActivity.setId(1L);

        StreamEntityDTO testDestinationStream = new StreamEntityDTO();
        testDestinationStream.setId(1L);
        testDestinationStream.setUniqueIdentifier("group1");
        testDestinationStream.setType(EntityType.NOTSET);
        testActivity.setDestinationStream(testDestinationStream);

        sut.getFollowers(testActivity);
    }
}
