DROP TABLE IF EXIST 'shift';
DROP TABLE IF EXIST 'type';


CREATE TABLE `type` (
    `id` bigint NOT NULL,
    `name` varchar(200) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shift` (
    `id` bigint NOT NULL auto_increment,
   `owner` text NOT NULL,
   `description` text,
   `lead_operator` text,
   `on_shift_personal` text,
   `report` text,
   `type_id` bigint NOT NULL,
   `close_shift_user` text,
   `start_date` datetime NOT NULL,
   `end_date` datetime,
   PRIMARY KEY  (`id`),
   CONSTRAINT `type_id_fk` FOREIGN KEY (`type_id`) REFERENCES `type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


