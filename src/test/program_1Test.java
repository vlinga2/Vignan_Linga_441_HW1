import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.vms.Vm;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class program_1Test {

    private program_1_timeshared program_1;

    @Before
    public void start(){
        program_1 = new program_1_timeshared();
        program_1.cloudSim = new CloudSim();

    }

    @Test
    public void createDatacentersTest(){
        int count = 2;
        program_1.peProvisionerSimple = new PeProvisionerSimple();
        List<NetworkDatacenter> list =  program_1.createDatacenters(count,1,new ArrayList<NetworkDatacenter>());
        assertEquals(count,list.size());
    }

    @Test
    public void createCloudletsTest(){
        int count = 2;
        List<CustomCloudlet> list =  program_1.createCloudlets(count,1,CustomCloudlet.Type.REDUCER,new ArrayList<CustomCloudlet>());
        assertEquals(count,list.size());

    }

    @Test
    public void createVMsTest(){
        int count = 4;
        List<Vm> list =  program_1.createVMs(1,count,new ArrayList<Vm>());
        assertEquals(count,list.size());
    }

    @Test
    public void createPEsTest(){
        int count = 4;
        List<Pe> list =  program_1.createPEs(1,1,new ArrayList<Pe>(),count);
        assertEquals(count,list.size());
    }

    @Test
    public void createHostsTest(){

        int count = 4;
        List<Host> list =  program_1.createHosts(count,1,new ArrayList<Host>());
        assertEquals(count,list.size());
    }

}
