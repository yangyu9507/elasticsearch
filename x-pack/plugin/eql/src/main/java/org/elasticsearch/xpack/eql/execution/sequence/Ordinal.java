/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.eql.execution.sequence;

import java.util.Objects;

public class Ordinal implements Comparable<Ordinal> {

    final long timestamp;
    final Comparable<Object> tiebreaker;

    public Ordinal(long timestamp, Comparable<Object> tiebreaker) {
        this.timestamp = timestamp;
        this.tiebreaker = tiebreaker;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, tiebreaker);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        Ordinal other = (Ordinal) obj;
        return Objects.equals(timestamp, other.timestamp)
                && Objects.equals(tiebreaker, other.tiebreaker);
    }

    @Override
    public String toString() {
        return "[" + timestamp + "][" + (tiebreaker != null ? tiebreaker.toString() : "") + "]";
    }

    @Override
    public int compareTo(Ordinal o) {
        if (timestamp < o.timestamp) {
            return -1;
        }
        if (timestamp == o.timestamp) {
            if (tiebreaker != null) {
                if (o.tiebreaker != null) {
                    return tiebreaker.compareTo(o.tiebreaker);
                }
                // nulls are first - lower than any other value
                // other tiebreaker is null this one isn't, fall through 1
            }
            // null tiebreaker
            else {
                if (o.tiebreaker != null) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
        // if none of the branches above matched, this ordinal is greater than o
        return 1;
    }

    public Object[] toArray() {
        return tiebreaker != null ? new Object[] { timestamp, tiebreaker } : new Object[] { timestamp };
    }
}