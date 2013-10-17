DROP TABLE IF EXIST 'shift';

CREATE TABLE `shift` (
    `id` bigint NOT NULL auto_increment,
   `owner` text NOT NULL,
   `description` text,
   `lead_operator` text,
   `on_shift_personal` text,
   `report` text,
   `start_date` datetime NOT NULL,
   `end_date` datetime,
   PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shift_properties` (
    `shift_id` bigint NOT NULL,
    `key` text NOT NULL,
    `value` text NOT NULL,
    CONSTRAINT `shift_propertie_id_fk` FOREIGN KEY (`shift_id`) REFERENCES `shift` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
