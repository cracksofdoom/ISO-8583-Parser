package net.cracksofdoom.parser.application;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import net.cracksofdoom.parser.api.CompareRequest;
import net.cracksofdoom.parser.api.ParseRequest;
import net.cracksofdoom.parser.core.IsoMessageParser;

@Service
public class IsoMessageService {
    public record Difference(int id, String name, String left, String right, String status) {
    }

    public record Comparison(IsoMessageParser.Result left, IsoMessageParser.Result right,
            String mtiStatus, List<Difference> differences) {
    }

    public IsoMessageParser.Result parse(ParseRequest request) {
        if (Boolean.TRUE.equals(request.revealSensitive())) {
            throw new IllegalArgumentException("Revealing sensitive values is unavailable on the public API");
        }
        return new IsoMessageParser(request.fieldOverrides()).parse(request.message(),
                request.format() == null ? "ASCII_HEX_BITMAP" : request.format(),
                request.headerBytes() == null ? 0 : request.headerBytes(), false);
    }

    public Comparison compare(CompareRequest request) {
        IsoMessageParser.Result left = parse(request.left());
        IsoMessageParser.Result right = parse(request.right());
        List<Difference> differences = new ArrayList<>();
        java.util.Set<Integer> ids = new TreeSet<>(left.fields().keySet());
        ids.addAll(right.fields().keySet());
        for (int id : ids) {
            var a = left.fields().get(id);
            var b = right.fields().get(id);
            String av = a == null ? null : a.value();
            String bv = b == null ? null : b.value();
            boolean hidden = IsoMessageParser.SENSITIVE_FIELDS.contains(id);
            differences.add(new Difference(id, a != null ? a.name() : b.name(), av, bv,
                    a == null ? "RIGHT_ONLY"
                            : b == null ? "LEFT_ONLY" : hidden ? "HIDDEN" : av.equals(bv) ? "MATCH" : "CHANGED"));
        }
        return new Comparison(left, right, left.mti().equals(right.mti()) ? "MATCH" : "CHANGED", differences);
    }
}
