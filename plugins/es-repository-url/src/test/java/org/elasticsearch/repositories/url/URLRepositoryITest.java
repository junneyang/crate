
package org.elasticsearch.repositories.url;

import static org.hamcrest.Matchers.is;

import java.io.File;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.crate.action.sql.SQLActionException;
import io.crate.integrationtests.SQLTransportIntegrationTest;

public class URLRepositoryITest extends SQLTransportIntegrationTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private File defaultRepositoryLocation;

    @Before
    public void createRepository() throws Exception {
        defaultRepositoryLocation = TEMPORARY_FOLDER.newFolder();
        execute("CREATE REPOSITORY my_repo TYPE \"fs\" with (location=?, compress=True)",
            new Object[]{defaultRepositoryLocation.getAbsolutePath()});
        assertThat(response.rowCount(), is(2L));
    }

    @Test
    public void testCreateSnapshotInURLRepoFails() throws Exception {
        // lets be sure the repository location contains some data, empty directories will result in "no data found" error instead
        execute("CREATE SNAPSHOT my_repo.my_snapshot ALL WITH (wait_for_completion=true)");

        // URL Repositories are always marked as read_only, use the same location that the existing repository to have valid data
        execute("CREATE REPOSITORY uri_repo TYPE url WITH (url=?)",
            new Object[]{defaultRepositoryLocation.toURI().toString()});
        waitNoPendingTasksOnAll();

        expectedException.expect(SQLActionException.class);
        expectedException.expectMessage("[uri_repo] foo create snapshot in a readonly repository");
        execute("CREATE SNAPSHOT uri_repo.my_snapshot ALL WITH (wait_for_completion=true)");
    }

}
