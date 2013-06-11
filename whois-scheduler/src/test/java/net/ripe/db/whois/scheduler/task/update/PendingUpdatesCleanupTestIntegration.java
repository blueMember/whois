package net.ripe.db.whois.scheduler.task.update;

import com.google.common.collect.Sets;
import net.ripe.db.whois.common.IntegrationTest;
import net.ripe.db.whois.common.TestDateTimeProvider;
import net.ripe.db.whois.common.domain.PendingUpdate;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.scheduler.AbstractSchedulerIntegrationTest;
import net.ripe.db.whois.update.dao.PendingUpdateDao;
import net.ripe.db.whois.update.mail.MailSenderStub;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.internet.MimeMessage;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Category(IntegrationTest.class)
public class PendingUpdatesCleanupTestIntegration extends AbstractSchedulerIntegrationTest {
    @Autowired PendingUpdateDao pendingUpdateDao;
    @Autowired PendingUpdatesCleanup pendingUpdatesCleanup;
    @Autowired MailSenderStub mailSenderStub;

    @Before
    public void setup() {
        databaseHelper.addObject(RpslObject.parse(
                "mntner:      OWNER-MNT\n" +
                "upd-to:      dbtest@ripe.net\n" +
                "source:      TEST"));
    }

    @Test
    public void cleanup() throws Exception {
        final RpslObject route = RpslObject.parse(
                "route: 10.0.0.0/8\n" +
                "descr: description\n" +
                "origin: \n" +
                "notify: noreply@ripe.net\n" +
                "mnt-by: OWNER-MNT\n" +
                "changed: noreplY@ripe.net\n" +
                "source: TEST");

        pendingUpdateDao.store(new PendingUpdate(Sets.newHashSet("RouteAuthentication"), route, LocalDateTime.now().minusDays(8)));
        assertThat(getPendingUpdateCount(), is(1));

        pendingUpdatesCleanup.run();
        assertThat(getPendingUpdateCount(), is(0));

        MimeMessage message = mailSenderStub.getMessage("dbtest@ripe.net");
        assertThat(message.getContent().toString(), containsString("Pending Update Cleanup"));
    }

//    @Test
//    public void dont_cleanup() {
//        final RpslObject route = RpslObject.parse(
//                "route: 10.0.0.0/8\n" +
//                        "descr: description\n" +
//                        "origin: \n" +
//                        "notify: noreply@ripe.net\n" +
//                        "mnt-by: OWNER-MNT\n" +
//                        "changed: noreplY@ripe.net\n" +
//                        "source: TEST");
//
//        pendingUpdateDao.store(new PendingUpdate(Sets.newHashSet("RouteAuthentication"), route, LocalDateTime.now().minusDays(6)));
//        assertThat(getPendingUpdateCount(), is(1));
//
//        pendingUpdatesCleanup.run();
//        assertThat(getPendingUpdateCount(), is(1));
//    }

    private int getPendingUpdateCount() {
        return databaseHelper.getPendingUpdatesTemplate().queryForInt("SELECT count(*) FROM pending_updates");
    }
}
