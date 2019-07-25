import { Reflection } from './reflection';
import { Base } from './base';
import { ReflectionQuestion } from './reflection-question';

export class Question extends Base {
  id?: number;
  body: string;
  category: string;
  forms: ReflectionQuestion[];
}
