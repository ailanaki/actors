package org.example.model;

import java.util.List;
import java.util.Objects;

public class Responce {
    public String host;
    public List<String> result;

    public Responce(String host, List<String> result) {
        this.host = host;
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Responce response = (Responce) o;
        return Objects.equals(result, response.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }
}