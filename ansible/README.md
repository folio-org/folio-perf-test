Ansible
-------


*  roles/ - CI-specific Ansible roles. 

*  inventory/ - AWS dynamic inventory configuration and script for FOLIO CI.

*  group_vars/ - Ansible group vars. See inventory/groups to see examples of
  how AWS tags are used to form Ansible groups.

*  folio-ansible/ - A git submodule of the folio-org/folio-ansible.  Many roles
defined from folio-ansible are reused for CI.

*  'folio*.yml' - Various top-level Ansible plays.


Some secrets in the playbook are encrypted with Ansible Vault so the password
stored in '.vault_pass.txt' or similar plain text file is required.

  Example Usage: 

```
   ansible-playbook -i inventory folioci.yml --vault-password-file ~/.vault_pass.txt

```


