# KT Advance SonarQube Plugin
[![Build Status](https://travis-ci.org/kestreltechnology/sonar-kt-advance.svg?branch=master)](https://travis-ci.org/kestreltechnology/sonar-kt-advance)

![Quality Status](https://sonarcloud.io/api/project_badges/measure?project=org.sonar.plugins.kt.advance%3Asonar-kt-advance-plugin%3Aswitch-to-2018-XMLs-format&metric=alert_status)
 
 


## 2018 upd:
depends on https://github.com/mrbkt/xml-kt-advance-java [![Build Status](https://travis-ci.org/mrbkt/xml-kt-advance-java.svg?branch=master)](https://travis-ci.org/mrbkt/xml-kt-advance-java)



# Usage
## Installation steps

0. Download and install [SonarQube 5.6.4](https://sonarsource.bintray.com/Distribution/sonarqube/sonarqube-5.6.4.zip)

1. Download the latest plug-in jar-file ( https://github.com/kestreltechnology/sonar-kt-advance/releases/latest ). 
Put the downloaded jar into `$SONARQUBE_HOME/extensions/plugins`, removing any previous versions of the same plugin. For more details refer https://docs.sonarqube.org/display/SONAR/Installing+a+Plugin


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

4. Install SonarQube command-line tools (`sonar-scanner CLI`)
Download version 2.8 from here https://sonarsource.bintray.com/Distribution/sonar-scanner-cli/

## Running the scanner
 
Before the first scanner run, put `sonar-project.properties` file into the root dir of you C project. A sample `sonar-project.properties` file could be found here: [here](docs/sample.sonar-project.properties).  
Please mind the `sonar.exclusions` property.


  - Note 1:  after sonar-scanner has finished its task, the SonarQube should perform some internal work (navigate to Administration/Background Tasks section to check if there are active tasks). For instance, the entire Redis analysis lists about 180000 issues, thus SonarQube takes 5-10 minutes before you can see any results on UI.

  - Note 2: SQ's display of issues on a file is limited to 100. To soften this unlovely feature, the rules in `KT Advance (discharged)` repository are disabled by default, thus discharged proof obligations are not submitted into SonarQube.


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
2 | `l` | PO **l**evel (`primary` or `secondary`)
3 | `t` | predicate **t**ype (AKA **t**ag; e.g. `pointer-cast`, `ptr-upper-bound`,...)

Each parameter is taken with configurable **magnitude** `m_X` (AKA *scale factor*):

<code>		effort = s<sup>m_s</sup> * l<sup>m_l</sup> * t<sup>m_t</sup>  </code>

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
`SQ` | `t` |  Predicate type Multiplier | `<predicate type>`  | By predicate-**t**ype multiplier. There are 31 by-predicate coefficients, which could be tuned via (http://localhost:9000/settings?category=technicaldebt&subcategory=efforts.by.predicate). 
 
TODO: support dead-code POs 

##### NOTE. SonarQube supports three ways of calculating remediation cost.
Here's the quote from Sonar's API docs. :

> 1. **Linear** - Each issue of the rule costs the same amount of time (coefficient) to fix.
> 2. **Linear with offset** - It takes a certain amount of time to analyze the issues of such kind on the file (offset). Then, each issue of the rule costs the same amount of time (coefficient) to fix. Total remediation cost by file = offset + (number of issues x coefficient)
> 3. **Constant/issue** - The cost to fix all the issues of the rule is the same whatever the number of issues of this rule in the file. Total remediation cost by file = constant



# Contributing
## Making a release
To make a release, just add a git tag @ any stable branch you want to release.
The simpliest way is to create and publish an empty release placeholder via web interface here:
https://github.com/kestreltechnology/sonar-kt-advance/releases

Adding a tag triggers Travis-CI build ( https://travis-ci.org/kestreltechnology/sonar-kt-advance ). When the build is done, Travis adds artefacts (jar file) to GitHub' release.

## Building
#### requirements:
- jdk 1.8+
- Maven 3.3.* or higher  


To build the plug-in, call 
```
mvn clean sass:update-stylesheets
mvn package
``` 
Please refer [redeploy.sh](redeploy.sh) file, which lists all the required steps.
