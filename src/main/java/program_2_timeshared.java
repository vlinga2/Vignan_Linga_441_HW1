import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cloudbus.cloudsim.allocationpolicies.*;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.network.switches.AggregateSwitch;
import org.cloudbus.cloudsim.network.switches.EdgeSwitch;
import org.cloudbus.cloudsim.network.switches.RootSwitch;
import org.cloudbus.cloudsim.network.topologies.BriteNetworkTopology;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerCompletelyFair;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class program_2_timeshared {

    public enum VmAllocationPolicy {
        BESTFIT,
        FIRSTFIT,
        WORSTFIT,
        SIMPLE,
        ROUNDROBIN
    }



    public enum Scheduler{
        SPACESHARED,
        TIMESHARED,
        COMPLETELYFAIR
    }


    private static Logger log = LoggerFactory.getLogger(program_1_spaceshared.class);
    private static String fileName = "configuration_2.";
    private static Config data =  ConfigFactory.load("configuration_2.conf");
    private static CloudSim cloudSim;
    private static String mod = "   ";
    private static List<CustomCloudlet> cloudletList;


    /**Contents:
     *  Datacenter, hosts, Vms and cloudlets creation.
     * simulati

     */
    private static VmAllocationPolicy vmAllocationPolicy;
    private static Scheduler cloudletScheduler;
    private static Scheduler vmScheduler;
    private static PeProvisionerSimple peProvisionerSimple;
    private static UtilizationModel utilizationModel;

    //Latency for network topology
    private static int LATENCY;

    // Mappers to reducers ratio
    private static int MAP_RED_RATIO;


    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        //initialize allocation and scheduler policies
        vmAllocationPolicy = VmAllocationPolicy.BESTFIT;
        cloudletScheduler = Scheduler.TIMESHARED;
        vmScheduler = Scheduler.SPACESHARED;
        peProvisionerSimple = new PeProvisionerSimple();
        utilizationModel = new UtilizationModelFull();
        MAP_RED_RATIO = 3;
        LATENCY = 1000000;
        cloudSim = new CloudSim();

        int vmCount = data.getInt(fileName +"vmCount");
        int dcCount = data.getInt(fileName +"dcCount");
        int mappersCount = data.getInt(fileName+"mappersCount");
        int reducersCount = data.getInt(fileName +"reducersCount");


        //creating mappers and reducers
        List<CustomCloudlet> mappers = createCloudlets(mappersCount,0,  CustomCloudlet.Type.MAPPER, new ArrayList<CustomCloudlet>());
        List<CustomCloudlet> reducers = createCloudlets(reducersCount, mappersCount, CustomCloudlet.Type.REDUCER,new ArrayList<CustomCloudlet>());
        cloudletList = new ArrayList<>();
        cloudletList.addAll(mappers);       cloudletList.addAll(reducers);
//        mappers.get(0).assignToDatacenter();

        //creating Datacenters
        List<NetworkDatacenter> datacenters = createDatacenters(dcCount,0,new ArrayList<NetworkDatacenter>());
//        datacenters.get(0).processEvent(brokerPolicy2);


        // if hosts in a datacenter exceed EdgeSwitch.PORTS * RootSwitch.PORTS * AggregateSwitch.PORTS, then create edge switches that match host size.
        if (datacenters.get(0).getHostList().size()> EdgeSwitch.PORTS * RootSwitch.PORTS * AggregateSwitch.PORTS){
            for(Host i:datacenters.get(0).getHostList()){

                datacenters.get(0).addSwitch(new EdgeSwitch(cloudSim,datacenters.get(0)));
            }
        }

        brokerPolicy2 brokerPolicy  = new brokerPolicy2(cloudSim,"broker2");
        int myBrokerId = (int) brokerPolicy.getId();
        brokerPolicy.setMappers(mappers);
        brokerPolicy.setReducers(reducers);
        brokerPolicy.setMapRedRatio(MAP_RED_RATIO);
        List<Vm> vms = createVMs(myBrokerId, vmCount,new ArrayList<Vm>());
        brokerPolicy.submitVmList(vms);
        brokerPolicy.submitCloudletList(mappers);

        //BriteNetworkTopology with LATENCY
        BriteNetworkTopology briteNetworkTopology = new BriteNetworkTopology();
        briteNetworkTopology.addLink(datacenters.get(0).getId(),brokerPolicy.getId(),1000,LATENCY);
        briteNetworkTopology.addLink(datacenters.get(1).getId(),brokerPolicy.getId(),1000,LATENCY);

        log.debug("Starting simulation");
        cloudSim.start();
        log.info("Simulation finished");

        getSummary(cloudletList);

    }

    static List<NetworkDatacenter> createDatacenters(int count,int id,List<NetworkDatacenter> datacenters){

        //if creation of datacenters got completed, return them
        if (count==0) return datacenters;

        NetworkDatacenter datacenter = createDatacenter("datacenter"+(count-1), id);
        log.info("Datacenter "+id+" with "+ datacenter.getHostList().size()+ " hosts CREATED");
        datacenters.add(datacenter);
        createDatacenters( count-1,id+1,datacenters);

        return datacenters;
    }



    static NetworkDatacenter createDatacenter(String name, int id){

        double cost = data.getDouble(fileName+name+".cost");
        double memCost = data.getDouble(fileName+name+".memCost") ;
        double StorageCost = data.getDouble(fileName+name+".StorageCost") ;
        double BwCost = data.getDouble(fileName+name+".BwCost") ;

        //hosts for datacenter
        int hostCount = data.getInt(fileName+name+".hostsCount");
        if (hostCount> EdgeSwitch.PORTS* AggregateSwitch.PORTS* RootSwitch.PORTS){

        }

        List<Host> hosts = createHosts(hostCount, id, new ArrayList<Host>());


        org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy policy = new VmAllocationPolicyBestFit();

        if (vmAllocationPolicy == VmAllocationPolicy.BESTFIT){
            policy = new VmAllocationPolicyBestFit();
        } else if(vmAllocationPolicy == VmAllocationPolicy.FIRSTFIT){
            policy = new VmAllocationPolicyFirstFit();
        } else if(vmAllocationPolicy == VmAllocationPolicy.SIMPLE){
            policy = new VmAllocationPolicySimple();
        } else if(vmAllocationPolicy == VmAllocationPolicy.ROUNDROBIN){
            policy = new VmAllocationPolicyRoundRobin();
        } else if(vmAllocationPolicy == VmAllocationPolicy.WORSTFIT){
            policy = new VmAllocationPolicyWorstFit();
        }
        NetworkDatacenter datacenter = new NetworkDatacenter(cloudSim,hosts, policy);
        datacenter.getCharacteristics()
                .setCostPerMem(memCost)
                .setCostPerStorage(StorageCost)
                .setCostPerBw(BwCost)
                .setCostPerSecond(cost);



        return datacenter;
    }

    static VmAllocationPolicy getVmAllocationPolicy(){
        return vmAllocationPolicy;
    }

    static PeProvisionerSimple getPeProvisionerSimple(){
        return peProvisionerSimple;
    }

    static UtilizationModel getUtilizationModel (){
        return  utilizationModel;
    }

    static int getMapRedRatio(){
        return MAP_RED_RATIO;
    }

    static List<Host> createHosts(int count, int startId, ArrayList<Host> hosts) {

        if (count == 0) return new ArrayList<>();

        int mips = data.getInt(fileName +"host.mips");
        int PECount = data.getInt(fileName +"host.cpuCount");

        List<Pe> pes = createPEs( 0, mips, new ArrayList<Pe>(),PECount);

        //Create CPUs (Processing Units)
        int ram = data.getInt(fileName +"host.ram") ;// host memory (MB)
        int storage = data.getInt(fileName +"host.storage"); // host storage
        int bw = data.getInt(fileName +"host.bw"); //todo: finish adding disk speed

        //Host creation
        CustomHost host = new CustomHost( ram,bw,storage,pes);
        host.setId(startId);
        VmScheduler scheduler = null;
        if (vmScheduler == Scheduler.SPACESHARED){
            scheduler = new VmSchedulerSpaceShared();
        } else if (vmScheduler == Scheduler.TIMESHARED){
            scheduler = new VmSchedulerTimeShared();
        }

        host.setVmScheduler(scheduler);
        hosts.add(host);

        createHosts(count-1, startId+1, hosts);
        return hosts;
    }

    static List<Pe> createPEs(int id, int mips, List<Pe> pes, int count) {

        if (count==0) {
            return null;
        }

        //Add PE unit
        pes.add(new PeSimple(id, mips,getPeProvisionerSimple()));

        createPEs( id+1, mips, pes,count-1);

        return pes;
    }

    static List<Vm> createVMs(int startId, int count,List<Vm> vms) {

        if (count==0) {
            return null;
        }

        int mips = data.getInt(fileName +"vm.mips");
        int size = data.getInt(fileName +"vm.size");
        int ram = data.getInt(fileName +"vm.ram");
        int bw = data.getInt(fileName +"vm.bw");
        int pesCount = data.getInt(fileName +"vm.pesCount");

        // create VM, the CloudletSchedulerTimeShared policy
        CloudletScheduler scheduler = null;
        if (cloudletScheduler == Scheduler.SPACESHARED){
            scheduler = new CloudletSchedulerSpaceShared();
        } else if (cloudletScheduler == Scheduler.TIMESHARED){
            scheduler = new CloudletSchedulerTimeShared();
        } else if (cloudletScheduler == Scheduler.COMPLETELYFAIR){
            scheduler = new CloudletSchedulerCompletelyFair();
        }
        Vm vm = new VmSimple( mips, pesCount,scheduler);
        vm.setId(startId);
        vm.setRam(ram);
        vm.setBw(bw);
        vm.setSize(size);
        log.info("VM"+startId+" (mips="+mips+",size="+size+",ram="+ram+",bw="+bw+",numCPUS="+pesCount+") created");

        //Add VM to list
        vms.add(vm);

        createVMs(startId+1,count-1,vms);

        return vms;
    }



    static DatacenterBrokerSimple createBroker(CloudSim simulation) throws Exception {
        return new DatacenterBrokerSimple(simulation,"Broker");
    }


    static List<CustomCloudlet> createCloudlets(int count, int id, CustomCloudlet.Type cloudletType, List<CustomCloudlet> cloudlets){

        if (count == 0) return null;


        int pesCount = data.getInt(fileName +"cloudlet.pesCount"); // number of cpus
        int length = data.getInt(fileName +"cloudlet.length");
        int fileSize =  data.getInt(fileName +"cloudlet.fileSize");
        int outputSize = data.getInt(fileName +"cloudlet.outputSize");

        CustomCloudlet cloudlet  = new CustomCloudlet(length*2, pesCount);
        cloudlet.setType(cloudletType); //Set custom type
        cloudlet.setId(id);
        cloudlet.setFileSize(fileSize);
        cloudlet.setOutputSize(outputSize);
        cloudlet.setUtilizationModel(getUtilizationModel());
        cloudlet.setUtilizationModelBw(getUtilizationModel());
        cloudlet.setUtilizationModelCpu(getUtilizationModel());
        cloudlet.setUtilizationModelRam(getUtilizationModel());

        cloudlets.add(cloudlet);


        log.info("cloudlet "+id+" cloudletType "+cloudletType+" CREATED");


        createCloudlets( count-1,  id+1,cloudletType, cloudlets);

        return cloudlets;
    }

    static  void getSummary(List<CustomCloudlet> list){
        log.info("cloudlet scheduler "+cloudletScheduler.toString()+" vmScheduler "+vmScheduler.toString()+" vmAllocationPolicy "+vmAllocationPolicy.toString());
        log.info("Cloudlet ID" + mod + "STATUS" + mod + "Data center ID" + mod + "Host ID" + mod + "Time" + mod + "Start Time" + mod + "Finish Time" + mod + "Cloudlet type" );

        //Call list item print function (functional programming technique)


        list.forEach(cloudlet -> printCloudletResult( cloudlet));

        double[] cost = computeTotalCost(list);
        log.info("Total cost: "+cost[0]+" wastage "+cost[1]+" delay "+cost[2]);

        //Now writing results to CSV file
        //writeResultsToCSV(list)
    }

    private static double[] computeTotalCost(List<CustomCloudlet> list){

        if (list.isEmpty()) {
            return new double[]{0.0, 0.0,0.0};
        }

        CustomCloudlet cloudlet = list.get(0);
        double costPerSec = 0;
        if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS)
            /*log.debug(cloudlet.getActualCpuTime()
                    +" "+cloudlet.getWaitingTime());*/
            costPerSec = cloudlet.getCostPerSec() *
                    (cloudlet.getSubmissionDelay()
                            +cloudlet.getActualCpuTime()
                            +cloudlet.getWaitingTime()
                            +cloudlet.getDelay()
                    )+cloudlet.getResWastageCost();
//                                cloudlet.getDelay());



        double[] temp = computeTotalCost(list.subList(1,list.size()));
        temp[0]+=costPerSec;
        temp[1]+=cloudlet.getResWastageCost();
        temp[2]+=cloudlet.getDelay();

        return temp;

    }

    private static void printCloudletResult(CustomCloudlet cloudlet ) {
        DecimalFormat dft = new DecimalFormat("###.##");
        if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {

            if (cloudlet.getType()== CustomCloudlet.Type.REDUCER) {
                log.info(mod + cloudlet.getId() + mod + mod + "  SUCCESS" + mod + mod + mod + mod +
                        +cloudlet.getLastTriedDatacenter().getId()+ mod + mod + mod
                        + cloudlet.getVm().getHost().getId() + mod + mod + mod +
                        dft.format(cloudlet.getActualCpuTime()) + mod + mod +
                        dft.format(cloudlet.getExecStartTime()) + mod + mod + mod +
                        dft.format(cloudlet.getFinishTime()) + mod + mod + mod + mod +
                        cloudlet.getType()+"("+cloudlet.getMappersSet()+")" + mod + mod + mod);// + cloudlet.getHost().getId());

            } else {
                log.info(mod + cloudlet.getId() +
                        mod + mod + "  SUCCESS" + mod + mod + mod + mod +
                        cloudlet.getLastTriedDatacenter().getId() + mod + mod + mod +
                        cloudlet.getVm().getHost().getId() + mod + mod + mod +
                        dft.format(cloudlet.getActualCpuTime()) + mod + mod +
                        dft.format(cloudlet.getExecStartTime()) + mod + mod + mod +
                        dft.format(cloudlet.getFinishTime()) + mod + mod + mod + mod +
                        cloudlet.getType () + mod + mod + mod
                );
            }

        } else {
            log.info(mod + cloudlet.getId() + mod + mod +cloudlet.getMappersSet()+ mod +cloudlet.getStatus());
        }
    }
}
