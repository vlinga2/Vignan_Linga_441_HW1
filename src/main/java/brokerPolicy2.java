import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**policy 2 :
 * a reducer gets submitted to broker if any of the host executes (MAP_RED_RATIO)no.of mappers. Then these mappers will be deleted from the host list.
 * After executing the above step, left over Mappers from all the Hosts will be submitted to reducer. here each mapper gets a delay as they are collected from different hosts.
 * Resource wastage cost:if less no. of mappers are allocated to a reducer than the MAP_RED_RATIO, then it involves a little resource wastage which will be added to the cloudlet.
 *
 */
public class brokerPolicy2 extends DatacenterBrokerSimple {

    private List<CustomCloudlet> submittedList = new ArrayList<>();
    private List<CustomCloudlet> finishedList = new ArrayList<>();


    private static final Logger log = LoggerFactory.getLogger(brokerPolicy1.class);
    private List<CustomCloudlet> mappers;
    private List<CustomCloudlet> reducers;
    private static int MAP_RED_RATIO = 3;
    private static Map<Host, List<CustomCloudlet>> hostMapperAllocation;
    private static int mappersFinshedCount = 0;
    private static int mappersSubmittedCount = 0;
    private static int reducersFinishedCount = 0;
    private static int reducersSubmittedCount = 0;


    public brokerPolicy2(CloudSim simulation, String name) throws Exception {
        super(simulation,name);
        hostMapperAllocation = new HashMap<>();
        mappers = new ArrayList<>();
        reducers = new ArrayList<>();
    }

    public void setMappers(List<? extends CustomCloudlet> list) {
        super.submitCloudletList(list);
        submittedList.addAll(list);
        mappers.addAll(list);
        mappersSubmittedCount+= mappers.size();
    }

    public void setReducers(List<? extends CustomCloudlet> list) {
        reducers.addAll(list);
        reducersSubmittedCount+=reducers.size();
    }



    private void submitCloudlets(CustomCloudlet reducer) {
        submittedList.add(reducer);
        submitCloudlet(reducer);
    }


    public List<CustomCloudlet> getMappers() {
        return mappers;
    }

    public List<CustomCloudlet> getReducers() {
        return reducers;
    }


    public List<CustomCloudlet> getSubmittedList() {
        return submittedList;
    }

    public List<CustomCloudlet> getFinishedList() {
        return finishedList;
    }
    //todo: added cloudlets get added to wrong VM
    private int vmIndex = 0;



    public void setMapRedRatio(int mapRedRatio) {
        MAP_RED_RATIO = mapRedRatio;
    }


    @Override
    public void processEvent(SimEvent ev) {


        switch (ev.getTag()) {
            case CloudSimTags.CLOUDLET_READY:
                Object obj1 = ev.getData();
                if (obj1!=null && obj1 instanceof CustomCloudlet) {
                    CustomCloudlet current1 = (CustomCloudlet) ev.getData();
                    log.debug(current1.getId()+" READY"+" "+current1.getStatus());
                }
                super.processEvent(ev);
                break;
            case CloudSimTags.CLOUDLET_SUBMIT:
                Object obj2 = ev.getData();
                if (obj2!=null && obj2 instanceof CustomCloudlet) {
                    CustomCloudlet current2 = (CustomCloudlet) ev.getData();
                    log.debug(current2.getId()+" SUBMIT"+" "+current2.getStatus());
                }
                super.processEvent(ev);
                break;
            case CloudSimTags.CLOUDLET_FINISH:
                Object obj3 = ev.getData();
                if (obj3!=null && obj3 instanceof CustomCloudlet){
                    CustomCloudlet current3 = (CustomCloudlet) ev.getData();
                    log.debug(current3.getId()+" FINISH"+" "+current3.getStatus());
                }


                super.processEvent(ev);
                break;

            case CloudSimTags.CLOUDLET_RETURN:

                CustomCloudlet cloudlet = (CustomCloudlet) ev.getData();
                if (cloudlet != null ){
                    if ((cloudlet).getType() == CustomCloudlet.Type.MAPPER){


                        mappers.remove(cloudlet);
                        mappersFinshedCount++;
                        log.debug("Mapper "+cloudlet.getId()+" status "+cloudlet.getStatus()+" mappersFinshedCount "+mappersFinshedCount+
                                " hostMapperAllocation.size "+hostMapperAllocation.size());
                        finishedList.add(cloudlet);

                        Host host = cloudlet.getVm().getHost();
                        if (hostMapperAllocation.containsKey(host)){
                            hostMapperAllocation.get(host).add(cloudlet);
                        } else {
                            hostMapperAllocation.put(host,new ArrayList<CustomCloudlet>());
                            hostMapperAllocation.get(host).add(cloudlet);
                        }

                        if (hostMapperAllocation.get(host).size()==MAP_RED_RATIO){
                            submitCloudlets(reducers.get(0));
                            List<Integer> mappersSet = new ArrayList<>();
                            hostMapperAllocation.get(host).forEach(mapper ->{
                                mappersSet.add((int) mapper.getId());
                                });
                            reducers.get(0).setMappersSet(mappersSet);
                            reducers.remove(0);
                            hostMapperAllocation.remove(host);
                        }
                    } else if ((cloudlet).getType() == CustomCloudlet.Type.REDUCER){
                        reducersFinishedCount++;
                        log.debug("Reducer "+cloudlet.getId()+" status "+cloudlet.getStatus()+" reducersFinishedCount "+reducersFinishedCount);

                        finishedList.add(cloudlet);

                    }

                    if (mappersFinshedCount == mappersSubmittedCount &&
                            submittedList.size() <( mappersSubmittedCount+reducersSubmittedCount)){

                        List<Integer> leftoverMappersSet = new ArrayList<>();
                        hostMapperAllocation.forEach((host,cloudlets)->{
                            cloudlets.forEach(leftoverMapper ->{
                                leftoverMappersSet.add((int) leftoverMapper.getId());
                                // as left over mappers from different hosts are being collected, add a slight delay.
                                leftoverMapper.addDelay(0.5D);
                                if (leftoverMappersSet.size() == MAP_RED_RATIO){
                                    reducers.get(0).setMappersSet(new ArrayList<>(leftoverMappersSet));
                                    submitCloudlets(reducers.get(0));
                                    reducers.remove(0);
                                    leftoverMappersSet.clear();
                                }
                            });
                        });

                        if (leftoverMappersSet.size() > 0){
                            submitCloudlets(reducers.get(0));
                            /* resWastageCost -> if less no. of mappers are allocated to a reducer than the conventional MAP_RED_RATIO,
                             then it involves a little resource wastage.

                            */
                            reducers.get(0).addResWastageCost((MAP_RED_RATIO-reducers.get(0).getMappersSet().size())*0.5D);
                            reducers.remove(0);
                            leftoverMappersSet.clear();
                        }
                        reducers.forEach(reducer ->{
                            submitCloudlets(reducer);
                            reducer.addResWastageCost((MAP_RED_RATIO-reducers.get(0).getMappersSet().size())*0.5D);
                        });
                        hostMapperAllocation.clear();

                        log.debug("remaining mappers "+leftoverMappersSet.size()+" submittedList.szie "+submittedList.size());
                    }

                }

                super.processEvent(ev);
                break;
            default:

                super.processEvent(ev);

        }
    }
}
