
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.Host;

import java.util.ArrayList;
import java.util.List;



/**
 * Custom Cloudlet class and contains extra parameters like:
 * cloudletType(MAPPER,REDUCER),
 * Mappers Set -> reducer's associated mappers
 * resWastageCost -> if less no. of mappers are allocated to a reducer than the conventional MAP_RED_RATIO, then it involves a little resource wastage.
 * delay -> if mappers from different hosts are left to be executed than the conventional same host reduction (broker policy 2), then it involves a little delay to tranfer them.
 */

public class CustomCloudlet extends CloudletSimple {


    public enum Type {
        MAPPER,
        REDUCER
    }

    private Type type = null;
    private Host host = null;
    private List<Integer> MappersSet = null;
    private Double delay = 0d;
    private Double resWastageCost = 0d;
    private NetworkDatacenter datacenter;

    public CustomCloudlet(long cloudletLength, int pesNumber) {
        super(cloudletLength,pesNumber);

    }

    public List<Integer> getMappersSet() {
        if (MappersSet == null)
            return new ArrayList<>();
        return MappersSet;
    }

    public void setMappersSet(List<Integer> mappersSet) {
        this.MappersSet = mappersSet;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Double getDelay() {
        return delay;
    }

    public void addDelay(Double d){
        delay+=d;
    }

    public void addResWastageCost(double d){
        resWastageCost +=d;
    }

    public void setDatacenter(NetworkDatacenter d){
        datacenter = d;
    }

    public NetworkDatacenter getDatacenter(){
        return datacenter;
    }

    public double getResWastageCost(){
        return resWastageCost;
    }

}
