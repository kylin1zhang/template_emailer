package com.citi.custody.service;

import com.citi.custody.entity.ReleaseInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReleaseService {

    public List<ReleaseInfo> getAllVersions() {
        // Mock data for demonstration purposes
        List<ReleaseInfo> releases = new ArrayList<>();
        ReleaseInfo release1 = new ReleaseInfo();
        release1.setProject("Project A");
        release1.setVersion("1.0.0");
        releases.add(release1);

        ReleaseInfo release2 = new ReleaseInfo();
        release2.setProject("Project B");
        release2.setVersion("2.0.0");
        releases.add(release2);

        return releases;
    }
}
