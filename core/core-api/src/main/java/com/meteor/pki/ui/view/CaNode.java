package com.meteor.pki.ui.view;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record CaNode(CertView cert, long leafCount, List<CaNode> children) {

    public static List<CaNode> forest(List<CertView> cas, List<CertView> leaves) {
        Map<String, List<CertView>> byIssuer = cas.stream()
            .filter(ca -> !ca.selfSigned())
            .collect(Collectors.groupingBy(CertView::akiHex));
        Map<String, Long> leafCounts = leaves.stream()
            .collect(Collectors.groupingBy(CertView::akiHex, Collectors.counting()));
        Set<String> knownSkis = cas.stream().map(CertView::skiHex).collect(Collectors.toSet());
        return cas.stream()
            .filter(ca -> ca.selfSigned() || !knownSkis.contains(ca.akiHex()))
            .sorted(Comparator.comparing(CertView::id))
            .map(ca -> node(ca, byIssuer, leafCounts))
            .toList();
    }

    private static CaNode node(CertView ca, Map<String, List<CertView>> byIssuer, Map<String, Long> leafCounts) {
        List<CaNode> children = byIssuer.getOrDefault(ca.skiHex(), List.of())
            .stream()
            .sorted(Comparator.comparing(CertView::id))
            .map(child -> node(child, byIssuer, leafCounts))
            .toList();
        return new CaNode(ca, leafCounts.getOrDefault(ca.skiHex(), 0L), children);
    }

}
