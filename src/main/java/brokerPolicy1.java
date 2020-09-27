
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**policy 1 :
 * (MAP_RED_RATIO)no.of mappers allocated to a reducer randomly before simulation.
 * a reducer gets submitted to broker iff it's associated mappers are done with the execution.
 * if less no. of mappers are allocated to a reducer than the MAP_RED_RATIO, then it involves a little resource wastage cost which will be added to the cloudlet.
 * Resource Wastage Cost : if a reducer takes the work from less no. of mappers than it's capacity, a little wastage of resource is found.
 */

public class brokerPolicy1 extends DatacenterBrokerSimple {

    private List<CustomCloudlet> submittedList = new ArrayList<>();
    private List<CustomCloudlet> finishedList = new ArrayList<>();


    private static final Logger log = LoggerFactory.getLogger(brokerPolicy1.class);
    private List<CustomCloudlet> mappers;
    private List<CustomCloudlet> reducers;
    static int MAP_RED_RATIO;
    static Map<CustomCloudlet, CustomCloudlet> mapRedAllocation;
    static int reducersFininshedCount = 0;

    private List<CustomCloudlet> leftOverMappers;
    private List<CustomCloudlet> leftOverReducers;

    public brokerPolicy1(CloudSim simulation, String name) throws Exception {
        super(simulation,name);
        mappers = new ArrayList<>();
        reducers = new ArrayList<>();
    }

    @Override
    public void startEntity() {
        mapRedAllocation = doMapRedAllocation(mappers,reducers,MAP_RED_RATIO);
        setMapperFinishListener();
        super.startEntity();
    }

    public void  setMappers(List<? extends CustomCloudlet> list) {
        super.submitCloudletList(list);
        submittedList.addAll(list);
        mappers.addAll(list);
        log.debug(mappers.size()+" Mappers submitted ");
    }

    public void setReducers(List<? extends CustomCloudlet> list) {
        reducers.addAll(list);
        log.debug(reducers.size()+" Reducers submitted ");
    }

    // Mapper finish listener which submits reducer iff all of it's mappers are done.
     void setMapperFinishListener(){

        int[] counter = new int[reducers.size()];
        for (int i=0;i<mappers.size();i++){
            mappers.get(i).addOnFinishListener(new EventListener<CloudletVmEventInfo>() {
                @Override
                public void update(CloudletVmEventInfo info) {
                    CustomCloudlet mapper = (CustomCloudlet) info.getCloudlet();
                    log.info("MAPPER "+mapper.getId()+" finished execution in datacenter "+mapper.getLastTriedDatacenter().getId());


                    CustomCloudlet reducer = mapRedAllocation.get(mapper);

                    // which stores the count of reducer's associated mappers which are done.
                    counter[(int) reducer.getId()-mappers.size()]++;

                    if (counter[(int) reducer.getId()-mappers.size()] == MAP_RED_RATIO){
                        submitReducer(reducer);
                        reducersFininshedCount++;
                        finishedList.add(reducer);
                    }
                    finishedList.add(mapper);

                    if ( reducersFininshedCount == reducers.size() - leftOverReducers.size()){
                        leftOverReducers.forEach(leftoverReducer ->{
                            submitReducer(leftoverReducer);

                            /* resWastageCost -> if less no. of mappers are allocated to a reducer than the conventional MAP_RED_RATIO,
                             then it involves a little resource wastage.
                            */
                            leftoverReducer.addResWastageCost((MAP_RED_RATIO-leftoverReducer.getMappersSet().size())*0.5D);
                            reducersFininshedCount++;
                        });
                    }
                }
            });



        }
    }


    // Mapper to Reducer allocation is done in this method
    Map<CustomCloudlet, CustomCloudlet> doMapRedAllocation(List<CustomCloudlet> mappers, List<CustomCloudlet> reducers, int ratio){
        Map<CustomCloudlet, CustomCloudlet> allocation = new HashMap<>();
        CustomCloudlet[] temp = new CustomCloudlet[reducers.size()];
        reducers.toArray(temp);
        int flag = reducers.size();
        for(int i=0;i<mappers.size();i++){
            int rand = (new Random()).nextInt(flag);
            List<Integer> list = temp[rand].getMappersSet();
            list.add((int) mappers.get(i).getId());
            temp[rand].setMappersSet(list);
            allocation.put(mappers.get(i),temp[rand]);


            log.debug(temp[rand].getId()+" getAssociatedMappers "+ temp[rand].getMappersSet());

            if (temp[rand].getMappersSet().size() == ratio){
                log.debug(temp[rand].getId()+" getAssociatedMappers "+ temp[rand].getMappersSet());
                flag--;
                CustomCloudlet t = temp[rand];
                temp[rand] = temp[flag];
                temp[flag] =  t;
            }
        }

        // if a reducer's associated mappers are less than MAP_RED_RATIO, they will be executed in the last with some added resource wastage
        if (flag>0) leftOverReducers = new ArrayList<>();
        for(int i=0;i<flag;i++){
            leftOverReducers.add(temp[i]);
        }

        return allocation;
    }

    //reducer submission to broker
    private void submitReducer(CustomCloudlet reducer) {
        submittedList.add(reducer);

        //reducer's onFinish listener
        reducer.addOnFinishListener(new EventListener<CloudletVmEventInfo>() {
            @Override
            public void update(CloudletVmEventInfo info) {
                CustomCloudlet reducer = (CustomCloudlet) info.getCloudlet();
                log.info("MAPPER "+reducer.getId()+" finished execution.");
            }
        });
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


    // Mappers to Reducers ratio
    public void setMapRedRatio(int mapRedRatio) {
        MAP_RED_RATIO = mapRedRatio;
    }

}
