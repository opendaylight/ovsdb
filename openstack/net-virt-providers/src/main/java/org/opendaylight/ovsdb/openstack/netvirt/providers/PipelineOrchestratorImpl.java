package org.opendaylight.ovsdb.openstack.netvirt.providers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by dave on 27/08/14.
 */
public class PipelineOrchestratorImpl implements PipelineOrchestrator {

  private Map<UUID, ServiceProperties> serviceRegistry = Maps.newHashMap();
  private Queue<Integer> registerQueue = Queues.newArrayBlockingQueue(16);
  private List<Integer> tableIdList = Lists.newArrayList(255);

  public UUID registerService(Integer altitude, ServiceDirection direction) {
    return UUID.randomUUID();
  }

  @Override
  public void unregisterService(UUID serviceID) {
    ServiceProperties sp = serviceRegistry.get(serviceID);
    List<Integer> registers = sp.getRegisters();
    if (registers != null) {
      for (Integer register : registers) {
        registerQueue.add(register);
      }
    }
    // Add Table ID allocation back

  }

  @Override
  public Integer getTableId(UUID serviceID) {
    ServiceProperties sp = serviceRegistry.get(serviceID);
    return sp.tableId;
  }

  @Override
  public Integer assignRegister(UUID serviceID) {
    Integer register = registerQueue.poll();
    if (register == null) {
      throw new RuntimeException("No registers remaining");
    }
    return register;
  }

  private Integer assignTableId(Integer altitude) {
    return null;
  }

  private class ServiceProperties {
    Integer altitude;
    ServiceDirection direction;
    Integer tableId;
    List<Integer> registers;

    public ServiceProperties(Integer altitude,
                             ServiceDirection direction,
                             Integer tableId,
                             List<Integer> registers) {
      this.altitude = altitude;
      this.direction = direction;
      this.tableId = tableId;
      this.registers = registers;
    }

    public Integer getAltitude() {
      return altitude;
    }

    public void setAltitude(Integer altitude) {
      this.altitude = altitude;
    }

    public ServiceDirection getDirection() {
      return direction;
    }

    public void setDirection(ServiceDirection direction) {
      this.direction = direction;
    }

    public Integer getTableId() {
      return tableId;
    }

    public void setTableId(Integer tableId) {
      this.tableId = tableId;
    }

    public List<Integer> getRegisters() {
      return registers;
    }

    public void setRegisters(List<Integer> registers) {
      this.registers = registers;
    }

    public void addRegister(Integer register) {
      this.registers.add(register);
    }

  }

}
