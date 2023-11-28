create table PyroErgebnis (
	SNPID MEDIUMINT NOT NULL AUTO_INCREMENT,
     chromosom varchar(10) not null,
     result varchar(3) not null,
     rsname varchar(15) not null,
     note bigint not null comment 'Positon',
     constraint IDPyroErgebnis primary key (SNPID));
   
     create table Sample_pyro (
     journal_number varchar(7) not null,
     SNPID varchar(15) not null,
     match_result varchar(5),
     SNP_to_journal_number MEDIUMINT NOT NULL AUTO_INCREMENT,
     constraint IDSample_has_pyro primary key (SNP_to_journal_number)
     );