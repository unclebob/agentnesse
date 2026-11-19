{
  "type": "test",
  "tags": ["sample"],
  "help": "A script that uses a scenario",
  "prune": false
}
# Login

| scenario | login as | user |
| --- | --- | --- |
| enter | @user | |
| login | | |

| script | Login |
| --- | --- | --- |
| login as | bob | |
| check | logged in | true |
