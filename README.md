Project Structure:

Simulations :
1. program_1_spaceshared : Simulation with brokerPolicy1 and space sharing.
2. program_1_timeshared  : Simulation with brokerPolicy1 and time sharing.
3. program_2_spaceshared : Simulation with brokerPolicy2 and space Sharing.
4. program_2_timeshared  : Simulation with brokerPolicy2 and time Sharing.
5. program_services      : Simulation of cloudlets in 3 DataCenters with mix of diff. service model implementations.

Installation/Usage Instructions:
* simply run the simulations program_1 and program_2's time shared and space shared variants.
* While running program_services simulation, you will be asked to input few customizations, you can skip them if you want.
  based on your responses it'll decide the Datacenter and prints the simulation summary.
  
Custom CloudSim classes:
*CustomCloudlet : it contains extra parameters like 
                  {cloudlet TYPE (Reducer, Mapper),
                  MappersSet,
                  delay,
                  resource wastage cost} which are explained later in this doc.
*brokerPolicy1  : Sub class to DatacenterBrokerSimple. this is one of the policy of the cloudlets(mapper,reducer) execution which is explained later .
                  contains extra methods like setMappers, set Reducers.
*brokerPolicy2  : Sub class to DatacenterBrokerSimple. this is one of the policy of the cloudlets(mapper,reducer) execution which is explained later.


Tests :

*brokerPolicy1Test : methods tested: 
                     setMappers() -> checks whether mappers which we set are same.
                     setReducers() -> checks whether reducers which we set are same.
                     
*program_1Test : methods tested:                   
                   createDatacenters() -> checks for the count of the datacenters created .
                   createCloudlets()   -> checks for the count of the cloudlets created .
                   createVMs()         -> checks for the count of the vms created .
                   createPEs()         -> checks for the count of the PEs created .
                   createHosts()       -> checks for the count of the Hosts created .


Configuration Parameters : 

Configurations for the above programs are present in resources folder.
parameters for these config files are as follows:  

*Datacenter : this contains datacenter's characteristics.
*Cloudlet   : this contains Cloudlet's characteristics.
*vm         : this contains vm's characteristics.
*Host       : this contains Host's characteristics.

Implemented Policies :

Two broker policies which differ by cloudlets(Mapper,Reeducer) execution. policies are as follows:

*brokerPolicy1 : 
* setMappers(),setReducers() - Receives mappers,Reducers but submits only mappers for execution, holds reducers.
* MAP_RED_RATIO a constant ratio which defines no.of mappers that must be allocated to a reducer.
* doMapRedAllocation()       - based on this ratio, mappers are allocated to random reducer before start of the simulation.if less no. of mappers are allocated to a reducer than the MAP_RED_RATIO,then it involves a little RESOURCE WASTAGE COST which will be added to the reducer after it's execution.
* setMapperFinishListener()  - adds on finish listener to all the mappers so that it's corresponding reducer will be submitted to the broker iff it's associated mappers are done. -> In short, Mapper to Redcuer allocation will be done based on the ratio and reducer starts executing iff all of it's                                
                    associated mappers are done executing.
                    
*brokerPolicy2 : 
* setMappers(),setReducers() - Receives mappers,Reducers but submits only mappers for execution, holds reducers.
* processEvent() - overrided superclass DatacenterBroker's method to know about cloudlet processing stage. a reducer will be submitted if (MAP_RED_RATIO) no. of mappers finish executing in the same host. if all mappers got finished, there might be few reducers left as corresponding mappers were executed on different hosts. In that case a slight DELAY to those mappers will be added.-> In short, each time if a host executes MAP_RED_RATIO no. of mappers, a reducer will be submitted to broker. 

5th step in HW :
*program_services : * 3 datacenters each for diff. mix of policies. policies are as follows
* Division of servies:
* SAAS - cloudlet config.
* PAAS - cloudlet config + cloudlet scheduler policy
* IAAS - DataCenter characteristics such as vm allocation policy.
* FAAS - (code snippet) cloudlet length
* 3 Datacenters and their services:
* Datacenter 0 : for mix of services SAAS && FAAS.
* Datacenter 1 : for mix of services PAAS && FAAS.
* Datacenter 2 : for IAAS.
* Takes input from user for few customizations like    
1)cloudlet Scheduler 2)cloudlet configurations ( length,file size and pes count) 3)Vm Allocation Policy.
                    * Based on the response received, user's cloudlets will be allocated to that respective Datacenter. 


Cost Computation :   
For simulations "program_1 and program_2" (brokerPolicy1, brokerPolicy2): 
* cost formula is given by cloudlet.getCostPerSec() *(cloudlet.getSubmissionDelay() +cloudlet.getActualCpuTime() +cloudlet.getWaitingTime()+cloudlet.getDelay())+cloudlet.getResWastageCost();
submissionDelay - cloudlets submission delay.
Delay - if a reducer's associated mappers got executed on diff. hosts.
ResWastageCost - if a reducer has less no. of associated mappers than usual MAP_RED_RATIO, then it involves a slight resource wastage cost. 

For simulations "program_services" (Step 5):
                    * cost formula is given by cloudlet.getCostPerSec() *(cloudlet.getSubmissionDelay() +cloudlet.getActualCpuTime()+cloudlet.getWaitingTime() )
                                                                          + cloudlet.getDatacenter().getCharacteristics().getCostPerSecond() * cloudlet.getUtilizationOfRam().                                                   
                    * Datacenter's cost per sec varies with service:
                    Datacenter 0 (mix of services SAAS && FAAS) being the cheapest and
                    Datacenter 2 (for IAAS) being the costliest.
                    
Analysis for program_1 and program_2:

cost evaluated from 4 simulations with the existing config file, 
*total cost for program_1_spaceshared ( brokerpolicy1 ): Total cost: 882.9199999999985 wastage 17.0  delay 0.0
*total cost for program_1_timeshared  ( brokerpolicy1 ): Total cost: 1076.210000000003 wastage 17.0 delay 0.0
*total cost for program_2_spaceshared  ( brokerpolicy2 ): Total cost: 957.5999999999981 wastage 18.0 delay 4.0
*total cost for program_2_timeshared  ( brokerpolicy2 ):  Total cost: 1075.342800000003 wastage 18.0 delay 4.0            

* Time shared vs Space shared : From the above 4 simulations, space shared from both the policies turns out to be the cheaper one.
                                As with the space shared scheduler, cloudlets won't be held for longer times (no concurrency) when there 
                                are less VMs to execute.
                                
* policy1 vs policy 2 : policy 1 and policy 2's timeshared simulation costed same whereas space shared simulation of
                        policy1 turns out be cheaper compared to policy 2. As a matter of fact, allocation is done prior 
                        to simulation in policy 1 with which almost each reducer fills up all it's slots whereas with 
                        policy 2, reducer's slots fill up only if mappers get executed on same host which is why policy 1
                        is cheaper compared to policy 2.

*Network Topology: Brite Network Topology is been used in our simulations, with the increase in latency, cost goes up
                   as cloudlet exec time increases.
                   
Analysis for program_services : 
There's not much to say about this simulation. If the customization increases, price goes up.  
