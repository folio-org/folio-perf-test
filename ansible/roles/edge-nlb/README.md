# edge-nlb

This role can be used to expose edge modules that can't be proxied through nginx (e.g. edge-connexion, edge-sip2). It will create one security group, one target group, one AWS NLB, and one Route 53 DNS alias per FOLIO instance, with the name based on the `folio_hostname` variable.

Do not use this role for nginx-proxied edge modules. Use the [../folio-elb](folio-elb) role instead.

## Role tasks

* Create a security group that allows access to the edge module ports
* Add the security group to the EC2 instances
* Remove any existing ALB and target group for the environment
* Create a target group with targets for each exposed port
* Create an AWS NLB with the new target group
* Add a route53 DNS entry for the load balancer

## Usage

Invoke this role once when building a FOLIO environment:

```yaml
- hosts: localhost
  gather_facts: no
  tasks:
    - name: configure NLB for non-HTTP edge modules
      include_role:
        name: edge-nlb
      vars:
        nlb_targets:
          - protocol: tcp
            port: 9000
        nlb_subnets:
          - subnet-c8df3dbe
          - subnet-4406021d
        route53_zone_name: dev.folio.org
```

## Variables
```yaml
---
# Defaults
aws_region: us-east-1

# Required variables without defaults

# jenkins_aws_access_key (string)
# jenkins_aws_secret_key (string)
# ec2_group (string)
# vpc_id (string)
# folio_hostname (string)
# nlb_targets (list/dictionary)
#   dictionary attributes: protocol, port (string)
# nlb_subnets (list/string)
# route53_zone_name (string)
```
