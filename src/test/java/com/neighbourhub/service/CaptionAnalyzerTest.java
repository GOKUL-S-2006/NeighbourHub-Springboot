package com.neighbourhub.service;

import com.neighbourhub.util.GeoUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaptionAnalyzerTest {

    @Test
    void largePotholeIsHighSeverityRoadIssue() {
        var r = CaptionAnalyzer.analyze("A large pothole in the middle of the road");
        assertEquals("road", r.category());
        assertEquals("High", r.severity());
        assertEquals("high", r.priority());
    }

    @Test
    void smallCrackIsMedium() {
        var r = CaptionAnalyzer.analyze("Small crack on the pavement");
        assertEquals("road", r.category());
        assertEquals("Medium", r.severity());
        assertEquals("medium", r.priority());
    }

    @Test
    void unknownTextIsGeneralLow() {
        var r = CaptionAnalyzer.analyze("Something odd");
        assertEquals("general", r.category());
        assertEquals("Low", r.severity());
    }

    @Test
    void haversineAndParsing() {
        double[] a = GeoUtils.parseLatLon("12.9716, 77.5946");
        assertNotNull(a);
        assertNull(GeoUtils.parseLatLon("not,a-location"));
        // ~111 m per 0.001 degree of latitude
        double d = GeoUtils.distanceMeters(a[0], a[1], a[0] + 0.001, a[1]);
        assertTrue(d > 100 && d < 120);
    }
}
