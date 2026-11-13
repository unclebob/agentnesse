{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A < search reference is rewritten when an ancestor or the target is renamed.

| script | | | |
| --- | --- | --- | --- |
| given page | RnBackTop | with content | top |
| given page | RnBackTop.RnBackTarget | with content | target |
| given page | RnBackTop.RnBackMiddle | with content | middle |
| given page | RnBackTop.RnBackMiddle.RnBackMidTarget | with content | mid |
| given page | RnBackTop.RnBackMiddle.RnBackRefer | with content | <RnBackTop.RnBackTarget <RnBackTop.RnBackMiddle.RnBackMidTarget |
| check | request page | RnBackTop?responder=renamePage&newName=RnBackTopNew&refactorReferences=on | 303 |
| page | RnBackTopNew.RnBackMiddle.RnBackRefer | should contain | href="RnBackTopNew.RnBackTarget" |
| page | RnBackTopNew.RnBackMiddle.RnBackRefer | should contain | href="RnBackTopNew.RnBackMiddle.RnBackMidTarget" |
| check | request page | RnBackTopNew.RnBackMiddle?responder=renamePage&newName=RnBackMidNew&refactorReferences=on | 303 |
| page | RnBackTopNew.RnBackMidNew.RnBackRefer | should contain | href="RnBackTopNew.RnBackTarget" |
| page | RnBackTopNew.RnBackMidNew.RnBackRefer | should contain | href="RnBackTopNew.RnBackMidNew.RnBackMidTarget" |
