/*
 * KT Advance
 * Copyright (c) 2016 Kestrel Technology LLC
 * http://www.kestreltechnology.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.kt.advance.batch;

public class EffortComputer {
    /**
     * <code>m_s</code>
     */
    private float stateScaleFactor;

    /**
     * Organization-specific scale factor for rules <br>
     * <code>m_r</code>
     */
    private float predicateScaleFactor;

    private float predicateTypeMultiplier;
    private float poLevelMultiplier = 1;
    private float poLevelScaleFactor = 1;

    /**
     * <code>s</code>
     */
    private float stateMultiplier = 1;

    private Integer complexityC;

    private Integer complexityP;

    private Integer complexityG;

    /**
     * rule-specific scale factor for complexities
     */
    private Float complexityCScaleFactor;

    private Float complexityPScaleFactor;

    private Float complexityGScaleFactor;

    public static float oneIfNull(Number x) {
        if (null == x) {
            return 1;
        }
        return x.floatValue();
    }

    public static double zeroIfNull(Number x) {
        if (null == x) {
            return 0;
        }
        return x.doubleValue();
    }

    private static double inc(Number x) {
        return zeroIfNull(x) + 1;
    }

    private static double pow(Number x, Float p) {
        return Math.pow(oneIfNull(x), oneIfNull(p));
    }

    public double compute() {
        return 1.0
                * pow(stateMultiplier, stateScaleFactor)
                * pow(predicateTypeMultiplier, predicateScaleFactor)
                * pow(poLevelMultiplier, poLevelScaleFactor)
                * pow(inc(complexityC), complexityCScaleFactor)
                * pow(inc(complexityP), complexityPScaleFactor)
                * pow(inc(complexityG), complexityGScaleFactor);
    }

    public Float getComplexityCScaleFactor() {
        return complexityCScaleFactor;
    }

    public Float getComplexityGScaleFactor() {
        return complexityGScaleFactor;
    }

    public Float getComplexityPScaleFactor() {
        return complexityPScaleFactor;
    }

    public float getGlobalEffortMultiplier() {
        return stateMultiplier;
    }

    public float getPoLevelMultiplier() {
        return poLevelMultiplier;
    }

    public float getPoLevelScaleFactor() {
        return poLevelScaleFactor;
    }

    public float getPredicateTypeMultiplier() {
        return predicateTypeMultiplier;
    }

    public float getStateScaleFactor() {
        return stateScaleFactor;
    }

    public void setComplexityC(Integer complexityC) {
        this.complexityC = complexityC;
    }

    public void setComplexityCScaleFactor(Float complexityCScaleFactor) {
        this.complexityCScaleFactor = complexityCScaleFactor;
    }

    public void setComplexityG(Integer complexityG) {
        this.complexityG = complexityG;
    }

    public void setComplexityGScaleFactor(Float complexityGScaleFactor) {
        this.complexityGScaleFactor = complexityGScaleFactor;
    }

    public void setComplexityP(Integer complexityP) {
        this.complexityP = complexityP;
    }

    public void setComplexityPScaleFactor(Float complexityPScaleFactor) {
        this.complexityPScaleFactor = complexityPScaleFactor;
    }

    public void setPoLevelMultiplier(float poLevelMultiplier) {
        this.poLevelMultiplier = poLevelMultiplier;
    }

    public void setPoLevelScaleFactor(float poLevelScaleFactor) {
        this.poLevelScaleFactor = poLevelScaleFactor;
    }

    public void setPredicateScaleFactor(float rulesScaleFactor) {
        this.predicateScaleFactor = rulesScaleFactor;
    }

    public void setPredicateTypeMultiplier(float predicateTypeMultiplier) {
        this.predicateTypeMultiplier = predicateTypeMultiplier;
    }

    public void setStateMultiplier(float globalEffortMultiplier) {
        this.stateMultiplier = globalEffortMultiplier;
    }

    public void setStateScaleFactor(float stateScaleFactor) {
        this.stateScaleFactor = stateScaleFactor;
    }

    @Override
    public String toString() {
        return "EffortComputer [stateScaleFactor=" + stateScaleFactor + ", predicateScaleFactor=" + predicateScaleFactor
                + ", predicateTypeMultiplier=" + predicateTypeMultiplier + ", poLevelMultiplier=" + poLevelMultiplier
                + ", poLevelScaleFactor=" + poLevelScaleFactor + ", stateMultiplier=" + stateMultiplier
                + ", complexityC=" + complexityC + ", complexityP=" + complexityP + ", complexityG=" + complexityG
                + ", complexityCScaleFactor=" + complexityCScaleFactor + ", complexityPScaleFactor="
                + complexityPScaleFactor + ", complexityGScaleFactor=" + complexityGScaleFactor + "]";
    }

}
