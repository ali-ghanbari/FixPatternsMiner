/**
 * Copyright (c) 2014, Thindeck.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the thindeck.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.thindeck.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.jcabi.dynamo.Frame;
import com.jcabi.dynamo.Item;
import com.jcabi.dynamo.Region;
import com.jcabi.dynamo.Table;
import com.jcabi.dynamo.Valve;
import com.thindeck.api.Repo;
import com.thindeck.api.Repos;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link DyRepos}.
 *
 * @author Krzysztof Krason (Krzysztof.Krason@gmail.com)
 * @version $Id$
 */
public final class DyReposTest {

    /**
     * DyRepos can get single repo by name.
     * @throws IOException In case of error.
     */
    @Test
    public void getRepoByName() throws IOException {
        final String name = "repo_name";
        final Repos repos = new DyRepos(this.region(name));
        MatcherAssert.assertThat(
            repos.get(name).name(), Matchers.is(name)
        );
    }

    /**
     * DyRepos throws exception on adding existing repo.
     * @throws IOException In case of error.
     */
    @Test(expected = IllegalArgumentException.class)
    public void addExistingRepo() throws IOException {
        final String name = "existing_repo_name";
        final Repos repos = new DyRepos(this.region(name));
        repos.add(name);
    }

    /**
     * DyRepos can return single repos.
     * @throws IOException In case of error.
     */
    @Test
    public void iteratesOverEmptyRepoList() throws IOException {
        final Iterator<Repo> repos = new DyRepos(this.region()).iterate()
            .iterator();
        MatcherAssert.assertThat(repos.hasNext(), Matchers.is(false));
    }

    /**
     * DyRepos can return single repos.
     * @throws IOException In case of error.
     */
    @Test
    public void iteratesOverSingleRepo() throws IOException {
        final String name = "repo name";
        final Iterator<Repo> repos = new DyRepos(this.region(name)).iterate()
            .iterator();
        MatcherAssert.assertThat(repos.next().name(), Matchers.equalTo(name));
        MatcherAssert.assertThat(repos.hasNext(), Matchers.is(false));
    }

    /**
     * DyRepos can return multiple repos.
     * @throws IOException In case of error.
     */
    @Test
    public void iteratesOverMultipleRepos() throws IOException {
        final String first = "first name";
        final String second = "second name";
        final Iterator<Repo> repos = new DyRepos(this.region(first, second))
            .iterate().iterator();
        MatcherAssert.assertThat(repos.next().name(), Matchers.equalTo(first));
        MatcherAssert.assertThat(repos.next().name(), Matchers.equalTo(second));
        MatcherAssert.assertThat(repos.hasNext(), Matchers.is(false));
    }
    /**
     * Create region with repos.
     * @param names Names of the repos.
     * @return Region created.
     * @throws IOException In case of error.
     */
    private Region region(final String... names) throws IOException {
        final Region region = Mockito.mock(Region.class);
        final Table table = Mockito.mock(Table.class);
        final Frame frame = Mockito.mock(Frame.class);
        Mockito.when(region.table(Mockito.eq(DyRepo.TBL))).thenReturn(table);
        Mockito.when(table.frame()).thenReturn(frame);
        Mockito.when(
            frame.where(
                Mockito.eq(DyRepo.ATTR_NAME), Mockito.any(String.class)
            )
        ).thenReturn(frame);
        Mockito.when(
            frame.where(
                Mockito.eq(DyRepo.ATTR_NAME), Mockito.any(Condition.class)
            )
        ).thenReturn(frame);
        Mockito.when(frame.through(Mockito.any(Valve.class))).thenReturn(frame);
        final Collection<Item> items = new LinkedList<Item>();
        for (final String name : names) {
            final AttributeValue attr = Mockito.mock(AttributeValue.class);
            Mockito.when(attr.getS()).thenReturn(name);
            final Item item = Mockito.mock(Item.class);
            Mockito.when(item.get(Mockito.eq(DyRepo.ATTR_NAME)))
                .thenReturn(attr);
            items.add(item);
        }
        Mockito.when(frame.iterator()).thenReturn(items.iterator());
        return region;
    }
}

