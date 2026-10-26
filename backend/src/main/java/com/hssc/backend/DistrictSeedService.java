package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DistrictSeedService {
    private final DistrictRepository districts; private final ZoneRepository zones;
    public DistrictSeedService(DistrictRepository districts,ZoneRepository zones){this.districts=districts;this.zones=zones;}
    @Transactional
    public void seed(String config){
        if(config==null || config.isBlank()) return;
        List<String[]> entries=new ArrayList<>();
        for(String districtEntry:config.split(";")){
            String[] parts=districtEntry.trim().split("=",2);
            if(parts.length!=2 || parts[0].isBlank()) throw new IllegalStateException("Invalid HSSC_DISTRICT_ZONES entry: "+districtEntry);
            String[] districtAndZones=parts[1].split("\\|",2);
            if(districtAndZones.length!=2 || districtAndZones[0].isBlank() || districtAndZones[1].isBlank())
                throw new IllegalStateException("Each district entry must be CODE=Name|Zone One,Zone Two");
            if(Arrays.stream(districtAndZones[1].split(",")).anyMatch(z->z.isBlank()))
                throw new IllegalStateException("Zone names cannot be blank");
            entries.add(new String[]{parts[0].trim(),districtAndZones[0].trim(),districtAndZones[1]});
        }
        for(String[] entry:entries){
            District district=districts.findByCode(entry[0]).orElseGet(()->districts.save(new District(entry[0],entry[1])));
            Set<String> existing=new HashSet<>();
            zones.findByDistrictId(district.getId()).forEach(z->existing.add(z.getName().toLowerCase(Locale.ROOT)));
            for(String zoneName:entry[2].split(",")){
                String zone=zoneName.trim();
                if(existing.add(zone.toLowerCase(Locale.ROOT))) zones.save(new Zone(district,zone));
            }
        }
    }
}
