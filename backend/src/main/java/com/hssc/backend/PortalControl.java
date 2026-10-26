package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="portal_controls")
public class PortalControl {
    @Id @Column(length=80) private String key;
    @Column(nullable=false) private boolean enabled;
    protected PortalControl() {}
    public PortalControl(String key,boolean enabled){this.key=key;this.enabled=enabled;}
    public String getKey(){return key;} public boolean isEnabled(){return enabled;} public void setEnabled(boolean enabled){this.enabled=enabled;}
}
