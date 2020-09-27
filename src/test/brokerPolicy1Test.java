import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class brokerPolicy1Test {
    private brokerPolicy1 brokerPolicy1;
    private CloudSim simulation;

    @Before
    public void start() throws Exception {
        simulation = new CloudSim();
        brokerPolicy1 = new brokerPolicy1(simulation,"broker");
    }

    @Test
    public void setMappersTest(){
        int count = 4;
        List<CustomCloudlet> cloudlets = new ArrayList<>();
        brokerPolicy1.setMappers(cloudlets);
        assertEquals(brokerPolicy1.getMappers(),cloudlets);
    }


    @Test
    public void setReducersTest(){
        int count = 4;
        List<CustomCloudlet> cloudlets = new ArrayList<>();
        brokerPolicy1.setReducers(cloudlets);
        assertEquals(brokerPolicy1.getReducers(),cloudlets);
    }
}
