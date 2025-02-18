create table activity_type (
  model_id integer not null,
  name text not null,
  parameters merlin_parameter_set not null,
  required_parameters merlin_required_parameter_set not null,

  constraint activity_type_natural_key
    primary key (model_id, name),
  constraint activity_type_owned_by_mission_model
    foreign key (model_id)
    references mission_model
    on delete cascade
);

comment on table activity_type is e''
  'A description of a parametric activity type supported by the associated mission model.';

comment on column activity_type.name is e''
  'The name of this activity type, unique within a mission model.';
comment on column activity_type.model_id is e''
  'The model defining this activity type.';
comment on column activity_type.parameters is e''
  'The set of parameters accepted by this activity type.';
