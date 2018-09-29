package org.sonatype.nexus.integrationtests.nexus2351;

import java.io.File;
import java.io.IOException;

import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.DeployUtils;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

public class Nexus2351DisableRedeployMaven2Test
    extends AbstractNexusIntegrationTest
{

    private RepositoryMessageUtil repoUtil = null;

    private File artifact;

    private File artifactMD5;
    
    @Before
    public void setup()
        throws Exception
    {   
        artifact = this.getTestFile( "artifact.jar" );
        artifactMD5 = this.getTestFile( "artifact.jar.md5" );
    }

    public Nexus2351DisableRedeployMaven2Test()
        throws ComponentLookupException
    {
        this.repoUtil = new RepositoryMessageUtil( this.getXMLXStream(), MediaType.APPLICATION_XML, this
            .getRepositoryTypeRegistry() );
    }
    
    @Test
    public void testM2ReleaseAllowRedeploy()
        throws Exception
    {

        String repoId = this.getTestId() + "-testM2ReleaseAllowRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.RELEASE );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar" );

        // now test checksums
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5" );
    }
    
    
    @Test
    public void testM2ReleaseNoRedeploy()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2ReleaseNoRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar" );

        // checksum should work
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar.md5" );

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar" );
            Assert.fail( "expected TransferFailedException" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar" );
            Assert.fail( "expected TransferFailedException" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar.md5" );
            Assert.fail( "expected TransferFailedException" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }
    
    @Test
    public void testM2ReleaseNoRedeployMultipleVersions()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2ReleaseNoRedeployMultipleVersions";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.0/testM2ReleaseNoRedeployMultipleVersions-1.0.0.jar" );

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.0/testM2ReleaseNoRedeployMultipleVersions-1.0.0.jar" );
            Assert.fail( "expected TransferFailedException" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.1/testM2ReleaseNoRedeployMultipleVersions-1.0.1.jar" );

        try
        {
            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.1/testM2ReleaseNoRedeployMultipleVersions-1.0.1.jar" );
            Assert.fail( "expected TransferFailedException" );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }


    }

    @Test
    public void testM2ReleaseReadOnly()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2ReleaseReadOnly";

        this.createM2Repo( repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.RELEASE );

        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2ReleaseReadOnly/1.0.0/testM2ReleaseReadOnly-1.0.0.jar" );
            Assert.fail( "expected TransferFailedException" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }

        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseReadOnly-1.0.0.jar.md5" );
            Assert.fail( "expected TransferFailedException" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }

    }

    @Test
    public void testM2SnapshotAllowRedeploy()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2SnapshotAllowRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.SNAPSHOT );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-216.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-217.jar" );
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-218.jar" );

        // now for the MD5
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-217.jar.md5" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-218.jar.md5" );

        // now for just the -SNAPSHOT

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-SNAPSHOT.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-SNAPSHOT.jar" );

        // MD5
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-SNAPSHOT.jar.md5" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-SNAPSHOT.jar.md5" );

    }

    @Test
    public void testM2SnapshotNoRedeploy()
        throws Exception
    {
        String repoId = this.getTestId() + "-testM2SnapshotNoRedeploy";

        this.createM2Repo( repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.SNAPSHOT );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-20090729.054915-218.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-20090729.054915-219.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-20090729.054915-220.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-SNAPSHOT.jar" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifact,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-SNAPSHOT.jar" );
        
        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-SNAPSHOT.jar.md5" );

        DeployUtils.deployWithWagon(
            this.getContainer(),
            "http",
            this.getRepositoryUrl( repoId ),
            artifactMD5,
            "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-SNAPSHOT.jar.md5" );
    }

    @Test
    public void testM2SnapshotReadOnly()
        throws Exception
    {

        String repoId = this.getTestId() + "-testM2SnapshotReadOnly";

        this.createM2Repo( repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.SNAPSHOT );

        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-20090729.054915-218.jar" );
            Assert.fail( "expected TransferFailedException" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-20090729.054915-218.jar.md5" );
            Assert.fail( "expected TransferFailedException" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifactMD5,
                "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-SNAPSHOT.jar.md5" );
            Assert.fail( "expected TransferFailedException" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
        try
        {

            DeployUtils.deployWithWagon(
                this.getContainer(),
                "http",
                this.getRepositoryUrl( repoId ),
                artifact,
                "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-SNAPSHOT.jar" );
            Assert.fail( "expected TransferFailedException" );

        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    private void createM2Repo( String repoId, RepositoryWritePolicy writePolicy, RepositoryPolicy releasePolicy )
        throws Exception
    {   
        RepositoryResource repo = new RepositoryResource();

        repo.setId( repoId );
        repo.setBrowseable( true );
        repo.setExposed( true );
        repo.setRepoType( "hosted" );
        repo.setName( repoId );
        repo.setRepoPolicy( releasePolicy.name() );
        repo.setWritePolicy( writePolicy.name() );
        repo.setProvider( "maven2" );
        repo.setFormat( "maven2" );
        repo.setIndexable( false );

      this.repoUtil.createRepository( repo );        
    }
    
    @BeforeClass
    public static void clean()
        throws IOException
    {
        cleanWorkDir();
    }

}
