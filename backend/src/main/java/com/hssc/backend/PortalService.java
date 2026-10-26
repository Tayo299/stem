package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {
    private final PortalControlRepository controls; private final AuditService audit; private final DistrictRepository districts;
    public PortalService(PortalControlRepository controls,AuditService audit,DistrictRepository districts){this.controls=controls;this.audit=audit;this.districts=districts;}
    @Transactional(readOnly=true)
    public boolean enabled(String key){return controls.findById(key).map(PortalControl::isEnabled).orElse(false);}
    @Transactional
    public void set(String key,boolean enabled,UserAccount actor){
        if(!java.util.Set.of("registration","exam_access","result_review").contains(key)) throw ApiException.badRequest("Unknown portal control");
        PortalControl control=controls.findById(key).orElseGet(()->new PortalControl(key,false));
        control.setEnabled(enabled);controls.save(control);audit.record(actor,"PORTAL_CONTROL_"+(enabled?"ENABLED":"DISABLED"),"PortalControl",key);
    }
    @Transactional(readOnly=true)
    public boolean districtEnabled(String key,java.util.UUID districtId){
        return controls.findById("district:"+districtId+":"+key).map(PortalControl::isEnabled).orElse(false);
    }
    @Transactional
    public void setDistrict(String key,java.util.UUID districtId,boolean enabled,UserAccount actor){
        if(!java.util.Set.of("exam_access","result_review").contains(key)) throw ApiException.badRequest("Unknown district portal control");
        if(!districts.existsById(districtId)) throw ApiException.notFound("District not found");
        String controlKey="district:"+districtId+":"+key;
        PortalControl control=controls.findById(controlKey).orElseGet(()->new PortalControl(controlKey,false));
        control.setEnabled(enabled);controls.save(control);
        audit.record(actor,"DISTRICT_PORTAL_CONTROL_"+(enabled?"ENABLED":"DISABLED"),"District",districtId.toString());
    }
}
