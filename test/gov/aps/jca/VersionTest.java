package gov.aps.jca;

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {

    @Test
    public void parsesNumericVersionWithQualifier() {
        Version version = new Version("JCA", "Java", "2.4.12-SNAPSHOT");

        Assert.assertEquals(2, version.getMajorVersion());
        Assert.assertEquals(4, version.getMinorVersion());
        Assert.assertEquals(12, version.getMaintenanceVersion());
        Assert.assertEquals(0, version.getDevelopmentVersion());
    }

    @Test
    public void parsesLegacyDevelopmentVersion() {
        Version version = new Version("JCA", "Java", "1.2D3");

        Assert.assertEquals(1, version.getMajorVersion());
        Assert.assertEquals(2, version.getMinorVersion());
        Assert.assertEquals(0, version.getMaintenanceVersion());
        Assert.assertEquals(3, version.getDevelopmentVersion());
    }

    @Test
    public void parsesLegacyDevelopmentVersionWithMaintenance() {
        Version version = new Version("JCA", "Java", "1.2.3D4");

        Assert.assertEquals(1, version.getMajorVersion());
        Assert.assertEquals(2, version.getMinorVersion());
        Assert.assertEquals(3, version.getMaintenanceVersion());
        Assert.assertEquals(4, version.getDevelopmentVersion());
    }

    @Test
    public void defaultsToZerosOnInvalidVersion() {
        Version version = new Version("JCA", "Java", "abc");

        Assert.assertEquals(0, version.getMajorVersion());
        Assert.assertEquals(0, version.getMinorVersion());
        Assert.assertEquals(0, version.getMaintenanceVersion());
        Assert.assertEquals(0, version.getDevelopmentVersion());
    }
}

