
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import java.util.List;

public class CustomHost extends HostSimple {

    public CustomHost(long ram, long bw, long storage, List<? extends Pe> peList) {
        super(ram, bw, storage, (List<Pe>) peList);
    }

}
