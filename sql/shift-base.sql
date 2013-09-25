DROP TABLE IF EXIST 'shift';

CREATE TABLE `shift` (
    `id` bigint not null,
   `owner` text not null,
   `start_date` datetime not null,
   `end_date` datetime,
   PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;