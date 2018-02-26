# KT Advance SonarQube Plugin

## 2018 upd:

depends on https://github.com/compartia/xml-kt-advance-java

[![Build Status](https://travis-ci.com/mrbkt/kestreltech.svg?token=1L4UwqexWxqBGfsyymrm&branch=master)](https://travis-ci.com/mrbkt/kestreltech)

## Installation steps


0. Download and install [SonarQube 5.5](https://sonarsource.bintray.com/Distribution/sonarqube/sonarqube-5.5.zip)

1. Build the plug-in with `mvn package` command and install it. You may look into `redeploy.sh` file, which lists all the required steps.

2. Navigate to [**Quality Profiles**](http://localhost:9000/profiles) section and ensure that there exists **KT Advance way** profile. Select it and check if it has active rules.

>Small note aside: every Sonar’s project has many2many relations with Languages. There’s no C/C++ support out of the box, thus we
>- either have to define own C Language, which probably may conflict with existing C/C++ plug-ins,
>- OR we have to “invent" another fake language like “C-analysis”,
>- OR we should depend on 3rd-partie’s C/C++ plug-ins.

3. Navigate to [**Rules**](http://localhost:9000/coding_rules) section and check Repository filter on the left panel, you should see at least 3 repositories:
  - KT Advance (discharged)
  - KT Advance (open)
  - KT Advance (violations)  
By default, rules in 'KT Advance (discharged)' group are disabled, so discharged proof obligations will not not be listed.

## Running the scanner
To try the scanner on a small Redis src subset, in terminal window, change dir to `/src/test/resources/test_project/redis` and run `sonar-scanner` (In case you don’t have `sonar-scanner CLI`, please refer to (http://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) ).
To log more debug info into console, you may run the scanner in verbose mode: `sonar-scanner -X`.

Alternatively, you may run the scanner in `/src/test/resources/test_project/itc-benchmarks/01.w_Defects` subdir.

Before the first scanner run, put `sonar-project.properties` file into the root dir of you C project. A sample `.properties` file could be found here: [here](https://github.com/kestreltechnology/sonar-kt-advance/blob/master/src/test/resources/test_project/redis/sonar-project.properties)
Please mind the `sonar.exclusions` property.




  - Note 1:  after sonar-scanner has finished its task, the SonarQube itself should perform some internal work (navigate to Administration/Background Tasks section to check if there are active ones). For instance, the entire Redis analysis lists about 180000 issues, thus SonarQube takes 5-10 minutes before you can see any results on UI.

  - Note 2: SQ's display of issues on a file is limited to 100. To soften this unlovely feature, the rules in `KT Advance (discharged)` repository are disabled by default, thus discharged proof obligations are not submitted into Sonar Qube.


## Configuration/administration

### Widget
After scan is complete, you may add “KT Advance” and/or "KT Advance Bar Chart" widgets to the Dashboard.

### Rules

Each Issue corresponds to a Rule. There are 3 groups of rules (and therefore, 3 types of Issues):

1. **Open Proof obligations** -- the ones which are marked "Open" in PEV/SEV XML files.
2. **Discharged Proof Obligations** -- the ones which are marked "Discharged" in PEV/SEV files. These are reported as issues with "Info" severity level and possess no technical debt (By default, these issues are not submitted, because the rules of  'KT Advance (discharged)' are disabled).
3. **Proven Violations** -- these are marked "Discharged" in PEV/SEV XML files and have attribute `violation="true"`. These are reported as *bugs*.

### Technical Debt (remediation effort) computation
#### Parameters
The Technical Debt (TD) per issue is calculated according to this formula:

	TD = coefficient * effort
where
  - `coefficient` is hardcoded const with time units (`"10min"`)
  -  and the `effort` is calculated per issue with respect to the following parameters:

 No. | var | comment
---- | --- | -------
1 | `s` | PO **s**tate (`open`, `discharged`, `violation`)
2 | `c[X]` | predicate **c**omplexity (which is combination of 3 diff. params: `c-complexity`, `p-complexity` and `g-complexity`)
3 | `l` | PO **l**evel (`primary` or `secondary`)
4 | `t` | predicate **t**ype (AKA **t**ag; e.g. `pointer-cast`, `ptr-upper-bound`,...)

Each parameter is taken with configurable **magnitude** `m_X` (AKA *scale factor*):

<code>		effort = s<sup>m_s</sup> * l<sup>m_l</sup> * t<sup>m_t</sup> * c[c]<sup>m_c[c]</sup> * c[p]<sup>m_c[p]</sup> * c[g]<sup>m_c[g]</sup> </code>

#### Configurable Parameters

 scope | term | UI label  | key | comment | default value
------ | ---- | --------- | --- | ------- | -------------
`SQ` | `s` | Open PO Multiplier | `effort.open.multiplier` | `s` value for **open** Proof Obligations | `2.0`
`SQ` | `s` | Discharged PO Multiplier | `effort.discharged.multiplier` | `s` value for **discharged** Proof Obligations | `0.0`
`SQ` | `s` | Violations Multiplier | `effort.violation.multiplier` | `s` value for proven **violations** | `10.0`
`SQ` | `m_s` | PO State Scale Factor | `effort.state.scale` | PO **s**tate scale factor | `0.5`
`SQ` | `l` |  Primary PO Multiplier | `effort.primary.multiplier` | `l` value for **primary** proof obligations | `2.0`
`SQ` | `l` |  Secondary PO Multiplier | `effort.secondary.multiplier` | `l` value for **secondary** proof obligations | `4.0`
`SQ` | `m_l` |  PO Level Scale Factor | `effort.level.scale` | PO **l**evel scale factor | `0.5`
`SQ` | `t` |  Predicate type Multiplier | `<predicate type>`  | By predicate-**t**ype multiplier. There are 31 by-predicate coefficients, which could be tuned via (/settings?category=kt+advance). The effective list of predicates with default coefficients could be found at [predicates.tsv](https://github.com/kestreltechnology/sonar-kt-advance/blob/master/src/main/resources/predicates.tsv). | see predicates.tsv
`SQ` | `m_c[c]` | C-complexity Scale Factor | `effort.complexity.c.multiplier` | Scale Factor c-complexity (`c[c]`) | `0.5`
`SQ` | `m_c[p]` | P-complexity Scale Factor | `effort.complexity.p.multiplier` | Scale Factor p-complexity (`c[p]`) | `0.5`
`SQ` | `m_c[g]` | G-complexity Scale Factor | `effort.complexity.g.multiplier` | Scale Factor g-complexity (`c[g]`) | `0.5`


##### NOTE. SonarQube supports three ways of calculating remediation cost.
Here's the quote from Sonar's API docs. :

> 1. **Linear** - Each issue of the rule costs the same amount of time (coefficient) to fix.
> 2. **Linear with offset** - It takes a certain amount of time to analyze the issues of such kind on the file (offset). Then, each issue of the rule costs the same amount of time (coefficient) to fix. Total remediation cost by file = offset + (number of issues x coefficient)
> 3. **Constant/issue** - The cost to fix all the issues of the rule is the same whatever the number of issues of this rule in the file. Total remediation cost by file = constant
