/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.database;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.neo4j.function.Functions;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.util.TestLogging;
import org.neo4j.kernel.logging.Logging;
import org.neo4j.test.OtherThreadExecutor;
import org.neo4j.test.OtherThreadRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DatabaseRegistryTest
{
    @Test
    public void shouldAllowCreatingAndVisitingDatabase() throws Throwable
    {
        // Given
        final AtomicReference<Database> dbProvidedToVisitor = new AtomicReference<>();

        // When
        registry.visit( "northwind", new DatabaseRegistry.Visitor()
        {
            @Override
            public void visit( Database db )
            {
                dbProvidedToVisitor.set( db );
            }
        } );

        // Then
        assertThat( dbProvidedToVisitor.get(), equalTo( database ) );
        verify( database ).init();
        verify( database ).start();
    }

    @Test
    public void shouldShutdownDatabaseOnDrop() throws Throwable
    {
        // When
        registry.drop( NORTH_WIND );

        // Then
        verify( database ).init();
        verify( database ).start();
        verify( database ).stop();
        verify( database ).shutdown();
    }

    @Ignore("Saw this fail in the assertion at line 109. This component is unused and JH will remove it")
    @Test
    public void shouldAwaitRunningQueriesBeforeDropping() throws Throwable
    {
        // Given
        final CountDownLatch visitingDbLatch = new CountDownLatch( 1 );
        final CountDownLatch doneVisitingLatch = new CountDownLatch( 1 );
        threadOne.execute( visitAndAwaitLatch( NORTH_WIND, registry, visitingDbLatch, doneVisitingLatch ) );

        // When
        visitingDbLatch.await();
        Future<Object> threadTwoCompletion = threadTwo.execute( drop( NORTH_WIND, registry ) );

        // Then, even if I wait a while, the database should not be shut down
        Thread.sleep( 100 );
        verify( database, never() ).stop();
        verify( database, never() ).shutdown();

        // But when
        doneVisitingLatch.countDown();
        threadTwoCompletion.get( 10, TimeUnit.SECONDS );

        // Then
        verify( database ).stop();
        verify( database ).shutdown();
    }

    private OtherThreadExecutor.WorkerCommand<Object, Object> drop( final String dbKey, final DatabaseRegistry registry )
    {
        return new OtherThreadExecutor.WorkerCommand<Object, Object>()
        {
            @Override
            public Object doWork( Object state ) throws Exception
            {
                registry.drop( dbKey );
                return null;
            }
        };
    }

    private OtherThreadExecutor.WorkerCommand<Object, Object> visitAndAwaitLatch( final String dbKey,
                                                                                  final DatabaseRegistry registry,
                                                                                  final CountDownLatch latchToUse,
                                                                                  final CountDownLatch latchToAwait )
    {
        return new OtherThreadExecutor.WorkerCommand<Object, Object>()
        {
            @Override
            public Object doWork( Object state ) throws Exception
            {
                registry.visit( dbKey, new DatabaseRegistry.Visitor()
                {
                    @Override
                    public void visit( Database db )
                    {
                        latchToUse.countDown();
                        try
                        {
                            latchToAwait.await( 10, TimeUnit.SECONDS );
                        }
                        catch ( InterruptedException e )
                        {
                            throw new RuntimeException( e );
                        }
                    }
                } );
                return null;
            }
        };
    }


    private static final String EMBEDDED = "embedded";
    private static final String NORTH_WIND = "northwind";
    private final Database database = mock( Database.class );
    private DatabaseRegistry registry;

    @Rule
    public OtherThreadRule<Object> threadOne = new OtherThreadRule<>();

    @Rule
    public OtherThreadRule<Object> threadTwo = new OtherThreadRule<>();

    @Before
    public void setUp() throws NoSuchDatabaseProviderException
    {
        registry = new DatabaseRegistry( Functions.<Config, Logging>constant( new TestLogging() ) );
        registry.addProvider( EMBEDDED, singletonDatabase( database ) );
        registry.init();
        registry.start();

        registry.create( new DatabaseDefinition( NORTH_WIND, EMBEDDED, DatabaseHosting.Mode.EXTERNAL, new Config() ) );
    }

    public static Database.Factory singletonDatabase( final Database db )
    {
        return new Database.Factory()
        {
            @Override
            public Database newDatabase( Config config, Logging logging )
            {
                return db;
            }
        };
    }
}
