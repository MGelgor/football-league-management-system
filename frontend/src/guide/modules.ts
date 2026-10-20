import type { ComponentType } from 'react'
import Architecture from './modules/Architecture'
import CupLive from './modules/CupLive'
import DataModel from './modules/DataModel'
import DataSaves from './modules/DataSaves'
import Economy from './modules/Economy'
import ManagerMode from './modules/ManagerMode'
import MatchEngine from './modules/MatchEngine'
import Overview from './modules/Overview'
import Players from './modules/Players'
import SeasonCycle from './modules/SeasonCycle'
import Tactics from './modules/Tactics'
import * as meta from './meta'
import type { ModuleMeta } from './parts'

export interface GuideModule extends ModuleMeta {
  Content: ComponentType
}

export const GUIDE_MODULES: GuideModule[] = [
  { ...meta.overview, Content: Overview },
  { ...meta.architecture, Content: Architecture },
  { ...meta.dataModel, Content: DataModel },
  { ...meta.seasonCycle, Content: SeasonCycle },
  { ...meta.matchEngine, Content: MatchEngine },
  { ...meta.players, Content: Players },
  { ...meta.tactics, Content: Tactics },
  { ...meta.economy, Content: Economy },
  { ...meta.managerMode, Content: ManagerMode },
  { ...meta.cupLive, Content: CupLive },
  { ...meta.dataSaves, Content: DataSaves },
]
