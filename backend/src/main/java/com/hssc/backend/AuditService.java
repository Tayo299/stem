package com.hssc.backend;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuditService {
    private final AuditRepository audit;
    public AuditService(AuditRepository audit){this.audit=audit;}
    @Transactional public void record(UserAccount actor,String action,String type,String id){audit.save(new AuditEvent(actor,action,type,id));}
    @Transactional(readOnly=true)
    public java.util.List<Api.AuditRecord> latest(){
        return audit.findTop200ByOrderByCreatedAtDesc().stream().map(e->new Api.AuditRecord(e.getId(),
                e.getActor()==null?null:e.getActor().getId(),e.getAction(),e.getEntityType(),e.getEntityId(),e.getCreatedAt())).toList();
    }
}
