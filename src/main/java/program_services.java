import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cloudbus.cloudsim.allocationpolicies.*;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.Host;
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
import java.util.Scanner;

public class program_services {

    public enum VmAllocationPolicy {
        BESTFIT,
        FIRSTFIT,
        WORSTFIT,
        SIMPLE,
        ROUNDROBIN
    }


    public enum Scheduler {
        SPACESHARED,
        TIMESHARED,
        COMPLETELYFAIR
    }


    private static Logger log = LoggerFactory.getLogger(program_services.class);
    private static String fileName = "configuration_2.";
    private static Config data = ConfigFactory.load("configuration_services.conf");
    private static CloudSim cloudSim;
    private static String mod = "   ";
    private static List<CustomCloudlet> cloudletList;

    private static program_services.VmAllocationPolicy vmAllocationPolicy;
    private static program_services.Scheduler cloudletScheduler;
    private static program_services.Scheduler vmScheduler;
    private static PeProvisionerSimple peProvisionerSimple;
    private static UtilizationModel utilizationModel;

    private static int policy = -1;

    // Mappers to reducers ratio
    private static int MAP_RED_RATIO;


    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        Scanner sc = new Scanner(System.in);

        //initialize allocation and scheduler policies
        vmAllocationPolicy = program_services.VmAllocationPolicy.BESTFIT;
        cloudletScheduler = program_services.Scheduler.SPACESHARED;
        vmScheduler = program_services.Scheduler.SPACESHARED;
        peProvisionerSimple = new PeProvisionerSimple();
        utilizationModel = new UtilizationModelFull();
        MAP_RED_RATIO = 3;
        cloudSim = new CloudSim();

        int vmCount = data.getInt(fileName + "vmCount");

        int cloudletLength = -1;
        int pesCount = -1;
        int fileSize = -1;
        int cloudletCount = -1;
        int cloudlet_scheduler = -1;
        boolean isFaas = false,isSaas = false,isPaas = false,isIaas = false;
        System.out.println("\nDo you want to choose cloudlet scheduler? (y/n)");
        String dec = sc.next();
        if (dec.equals("y")) {
            System.out.println("\n 1 -> Time Shared \n 2 -> Space Shared");
            dec = sc.next();
            if (dec.equals("1")) {
                cloudletScheduler = Scheduler.TIMESHARED;
                cloudlet_scheduler = 1;
                isPaas = true;
            } else if (dec.equals("2")){
                cloudletScheduler = Scheduler.SPACESHARED;
                cloudlet_scheduler = 1;
                isPaas = true;
            }
        }

        System.out.println("Do you want custom cloudlet configuration ( length, file size and pes count? (y/n)");
        dec = sc.next();
        if (dec.equals("y")) {
            isSaas = true;

            System.out.println("cloudlet length : \n 1 -> 1500 \n 2 -> 1000 \n 3 -> 500 \n press n to keep default");
            dec = sc.next();
            if (dec.equals("1") || dec.equals("2") || dec.equals("3")) isFaas = true;
            if (dec.equals("1")) cloudletLength = 1000;
            else if (dec.equals("2")) cloudletLength = 1500;
            else if (dec.equals("3")) cloudletLength = 500;


            System.out.println("pes count :  \n 1 -> 1 \n 2 -> 2 \n 3 -> 3 \n press n to keep default");
            dec = sc.next();
            if (dec.equals("1")) pesCount = 1;
            else if (dec.equals("2")) pesCount = 2;
            else if (dec.equals("3")) pesCount = 3;


            System.out.println("file size : ");
            dec = sc.next();
            try {
                fileSize = Integer.parseInt(dec);
                isSaas = true;
            } catch (Exception e) {

            }
        }
        System.out.println("No. of cloudlets:  \n press n to keep default");
        dec = sc.next();
        if (!dec.equals("n")) {
            try {
                cloudletCount = Integer.parseInt(dec);
            } catch (Exception e) {

            }
        }
        //iff it's not any of the above services, ask for IAAS
        System.out.println("Do you want to set Vm Allocation policy to DataCenter's hosts? (y/n)");
        dec = sc.next();
        if (dec.equals("y")) {
            isIaas = true;

            System.out.println(" 1 -> FIRSTFIT \n 2 -> BESTFIT \n 3 -> WORSTFIT \n 4 -> SIMPLE \n 5 -> ROUNDROBIN");
            dec = sc.next();
            if (dec.equals("1")) vmAllocationPolicy = VmAllocationPolicy.FIRSTFIT;
            else if (dec.equals("2")) vmAllocationPolicy = VmAllocationPolicy.BESTFIT;
            else if (dec.equals("3")) vmAllocationPolicy = VmAllocationPolicy.WORSTFIT;
            else if (dec.equals("4")) vmAllocationPolicy = VmAllocationPolicy.SIMPLE;
            else if (dec.equals("5")) vmAllocationPolicy = VmAllocationPolicy.ROUNDROBIN;
        }
        log.info("------------------------------------start simulation------------------------------------------");
        //creating Datacenters
        List<NetworkDatacenter> datacenters = createDatacenters(3, 0, new ArrayList<NetworkDatacenter>());
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(cloudSim, "broker");
        List<Vm> vms = createVMs((int) broker.getId(), vmCount, new ArrayList<Vm>());
        /**
         * Division of servies:
         * SAAS - cloudlet config.
         * PAAS - cloudlet config + cloudlet scheduler policy
         * IAAS - DataCenter characteristics such as vm allocation policy.
         * FAAS - (code snippet) cloudlet length
         * 3 Datacenters and their services:
         * Datacenter 0 : for mix of services SAAS && FAAS.
         * Datacenter 1 : for mix of services PAAS && FAAS.
         * Datacenter 2 : for IAAS.
         */

        //based on user's choice, corresponding broker submits vms and cloudlets.
        // if user chooses  vm allocation policy of datacenter and sets custom cloudlet length, then Datacenter 2 is choosen
        if (isIaas){
            policy = 2;
            log.info(" Datacenter 2 is chosen which comes under mix of services (IAAS)");
            if (cloudletCount <= 0) cloudletCount  = 10;
            cloudletList = createCloudlets(cloudletCount, 0, cloudletLength, pesCount, fileSize, new ArrayList<CustomCloudlet>());

            //assigning cloudlets to datacenter 2
            cloudletList.forEach(cloudlet->{
                cloudlet.assignToDatacenter(datacenters.get(2));
                cloudlet.setDatacenter(datacenters.get(2));
            });

        } // if user chooses Vm's property cloudlet scheduler and cloudlet configuration, Datacenter 0 is chosen
        else if (isFaas || isSaas || !isPaas) {
            policy = 0;
            log.info(" Datacenter 0 is chosen which comes under mix of services (SAAS & FAAS) ");
            if (cloudletCount <= 0) cloudletCount = 10;
            cloudletList = createCloudlets(cloudletCount, 0, cloudletLength, pesCount, fileSize, new ArrayList<CustomCloudlet>());

            //assigning cloudlets to datacenter 0
            cloudletList.forEach(cloudlet->{
                cloudlet.assignToDatacenter(datacenters.get(0));
                cloudlet.setDatacenter(datacenters.get(0));
            });
        } // if user sets any of the cloudlet config. and vm allocation policy of datacenter, Daatcenter 1 is chosen
        else if (isPaas ) {
            policy = 1;
            log.info(" Datacenter 1 is chosen which comes under mix of services (PAAS & FAAS)");

            if (cloudletCount <= 0) cloudletCount  = 10;
            cloudletList = createCloudlets(cloudletCount, 0, cloudletLength, pesCount, fileSize, new ArrayList<CustomCloudlet>());
            cloudletList.forEach(cloudlet->{
                //assigning cloudlets to datacenter 1
                cloudlet.assignToDatacenter(datacenters.get(1));
                cloudlet.setDatacenter(datacenters.get(1));
            });

        }
        broker.submitCloudletList(cloudletList);
        broker.submitVmList(vms);


        log.debug("Starting simulation");
        cloudSim.start();
        log.info("Simulation finished");

        getSummary(cloudletList);

    }

    static List<NetworkDatacenter> createDatacenters(int count,int id,List<NetworkDatacenter> datacenters){

        //if creation of datacenters got completed, return them
        if (count==0) return datacenters;

        NetworkDatacenter datacenter = createDatacenter("datacenter"+(id), id);

        log.info("Datacenter "+id+" with "+ datacenter.getHostList().size()+ " hosts CREATED "+datacenter.getCharacteristics().getCostPerMem());
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

        List<Host> hosts = createHosts(hostCount, id, new ArrayList<Host>());


        org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy policy = new VmAllocationPolicyBestFit();

        if (vmAllocationPolicy == program_services.VmAllocationPolicy.BESTFIT){
            policy = new VmAllocationPolicyBestFit();
        } else if(vmAllocationPolicy == program_services.VmAllocationPolicy.FIRSTFIT){
            policy = new VmAllocationPolicyFirstFit();
        } else if(vmAllocationPolicy == program_services.VmAllocationPolicy.SIMPLE){
            policy = new VmAllocationPolicySimple();
        } else if(vmAllocationPolicy == program_services.VmAllocationPolicy.ROUNDROBIN){
            policy = new VmAllocationPolicyRoundRobin();
        } else if(vmAllocationPolicy == program_services.VmAllocationPolicy.WORSTFIT){
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

    static program_services.VmAllocationPolicy getVmAllocationPolicy(){
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
        if (vmScheduler == program_services.Scheduler.SPACESHARED){
            scheduler = new VmSchedulerSpaceShared();
        } else if (vmScheduler == program_services.Scheduler.TIMESHARED){
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
        if (cloudletScheduler == program_services.Scheduler.SPACESHARED){
            scheduler = new CloudletSchedulerSpaceShared();
        } else if (cloudletScheduler == program_services.Scheduler.TIMESHARED){
            scheduler = new CloudletSchedulerTimeShared();
        } else if (cloudletScheduler == program_services.Scheduler.COMPLETELYFAIR){
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


    static List<CustomCloudlet> createCloudlets(int count, int id,int length,int pesCount,int fileSize, List<CustomCloudlet> cloudlets){

        if (count == 0) return cloudlets;

        if (pesCount <= 0) pesCount = data.getInt(fileName +"cloudlet.pesCount"); // number of cpus
        if(length <= 0) length = data.getInt(fileName +"cloudlet.length");
        if(fileSize <=0 ) fileSize =  data.getInt(fileName +"cloudlet.fileSize");
        int outputSize = data.getInt(fileName +"cloudlet.outputSize");


        CustomCloudlet cloudlet  = new CustomCloudlet(length*2, pesCount);
        cloudlet.setId(id);
        cloudlet.setFileSize(fileSize);
        cloudlet.setOutputSize(outputSize);
        cloudlet.setUtilizationModel(getUtilizationModel());
        cloudlet.setUtilizationModelBw(getUtilizationModel());
        cloudlet.setUtilizationModelCpu(getUtilizationModel());
        cloudlet.setUtilizationModelRam(getUtilizationModel());


        cloudlets.add(cloudlet);
        createCloudlets( count-1,id+1,length,pesCount,fileSize, cloudlets);
        log.info("cloudlet "+id+" fileSize "+ fileSize+" length "+length+" pesCount "+ pesCount+" CREATED");

        return cloudlets;
    }

    static  void getSummary(List<CustomCloudlet> list){
        log.info("===============================================================================");
        String services = "";
        if (list.get(0).getDatacenter().getId() == 0) services+="SAAS && FAAS";
        else if (list.get(0).getDatacenter().getId() == 1) services+="PAAS && FAAS";
        else if (list.get(0).getDatacenter().getId() == 2) services+="IAAS";

        log.info(" services "+services+" ");
        log.info(" cloudlet scheduler "+cloudletScheduler.toString()+"\n vmScheduler "+vmScheduler.toString()+"\n vmAllocationPolicy "+vmAllocationPolicy.toString());
        log.info("Cloudlet ID" + mod + "STATUS" + mod + "Data center ID" + mod + "Host ID" + mod + "Time" + mod + "Start Time" + mod + "Finish Time"  );

        list.forEach(cloudlet -> getCloudletResult( cloudlet,list.size()));

        double[] cost = computeTotalCost(list);
        log.info("Total cost: "+cost[0]);

    }

    private static double[] computeTotalCost(List<CustomCloudlet> list){

        if (list.isEmpty()) {
            return new double[]{0.0, 0.0,0.0};
        }

        // cloudlet's cost is computed
        CustomCloudlet cloudlet = list.get(0);
        double cost = 0;
        if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS)
            cost = cloudlet.getCostPerSec() *
                    (cloudlet.getSubmissionDelay() +cloudlet.getActualCpuTime() +cloudlet.getWaitingTime() )
                    + cloudlet.getDatacenter().getCharacteristics().getCostPerSecond() * cloudlet.getUtilizationOfRam();



        double[] temp = computeTotalCost(list.subList(1,list.size()));
        temp[0]+=cost;
        temp[1]+=cloudlet.getResWastageCost();
        temp[2]+=cloudlet.getDelay();

        return temp;

    }

    private static void getCloudletResult(CustomCloudlet cloudlet, int size ) {
        DecimalFormat dft = new DecimalFormat("###.##");
        if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
            log.info( cloudlet.getId() + mod + mod + "  SUCCESS" + mod + mod + mod + mod +
                    +cloudlet.getLastTriedDatacenter().getId()+ mod + mod + mod
                    + cloudlet.getVm().getHost().getId() + mod + mod + mod +
                    dft.format(cloudlet.getActualCpuTime()) + mod + mod +
                    dft.format(cloudlet.getExecStartTime()) + mod + mod + mod +
                    dft.format(cloudlet.getFinishTime()));


        } else {
            log.info(mod + cloudlet.getId() + mod + mod +cloudlet.getStatus());
        }
    }
}
