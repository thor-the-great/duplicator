Duplicator is a comand line tool that supports copying of folders and files from source to destination. Initial use case is automated creation of backup copies. For instance, you can backup some data with complex structure to the cloud storage folder, like Google Drive or MS One Drive, and you don't have to change structure of your data or copy it manually.

Supported features (as for time of writting):

- several source folders, one destination folder
- "smart" identification of delta between source and destination - based on file size and modification date, if no changes nothing copied
- several source folders with identical bottom folder (lowest in hierarchy) supported - in destination such folders with be renamed with pattern ("folder_parentfolder", e.g. current/mails and home/backup/mails => mails_current and mails_backup)

Setup is done in a setting.properties file that must be in the same folder as duplicator jar. Scheduling must be done externaly, e.g. Windows Task Scheduler or cron. 

Features planned for future:
- some smart way of filtering in source folder
- two way synchronization, so if something is removed at source remove it at destination as well
- GUI for setup
