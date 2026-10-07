import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Activity, 
  HelpCircle, 
  Flame, 
  Calendar, 
  Clock, 
  Target, 
  ArrowRight, 
  TrendingUp, 
  RotateCcw,
  BookOpen,
  Info,
  Trophy,
  Lock
} from 'lucide-react';
import { useAchievements } from '../hooks/useQueries';
import { 
  RadarChart, 
  PolarGrid, 
  PolarAngleAxis, 
  PolarRadiusAxis, 
  Radar, 
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  BarChart,
  Bar,
  Legend
} from 'recharts';
import { readinessApi, ReadinessResponse, HeatmapResponse, TrendsResponse, ExamCountdown } from '../api/readinessApi';

export const InsightsPage: React.FC = () => {
  const navigate = useNavigate();
  const [readiness, setReadiness] = useState<ReadinessResponse | null>(null);
  const [heatmap, setHeatmap] = useState<HeatmapResponse | null>(null);
  const [trends, setTrends] = useState<TrendsResponse | null>(null);
  const [countdown, setCountdown] = useState<ExamCountdown | null>(null);
  const [trendDays, setTrendDays] = useState<number>(90);
  const [showFormula, setShowFormula] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const { data: achievements = [], isLoading: loadingAchievements } = useAchievements();

  useEffect(() => {
    let mounted = true;
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [rData, hData, tData, cData] = await Promise.all([
          readinessApi.getReadiness(),
          readinessApi.getHeatmap(),
          readinessApi.getTrends(trendDays),
          readinessApi.getCountdown()
        ]);
        if (mounted) {
          setReadiness(rData);
          setHeatmap(hData);
          setTrends(tData);
          setCountdown(cData);
        }
      } catch (err: any) {
        if (mounted) {
          setError(err?.message || 'Failed to load insights data');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };
    fetchData();
    return () => {
      mounted = false;
    };
  }, [trendDays]);

  const isEmpty = !loading && (readiness?.readiness === 0 || readiness?.perSubject.every(s => s.readiness === 0));

  const getMasteryColorClass = (mastery: number) => {
    switch (mastery) {
      case 5:
        return 'bg-ds-success text-ds-accent-contrast font-bold';
      case 4:
        return 'bg-ds-success/20 text-ds-success border border-ds-success/40';
      case 3:
        return 'bg-ds-info/20 text-ds-info border border-ds-info/40';
      case 2:
        return 'bg-ds-warning/20 text-ds-warning border border-ds-warning/40';
      case 1:
        return 'bg-ds-danger/20 text-ds-danger border border-ds-danger/40';
      case 0:
      default:
        return 'bg-ds-surface-raised text-ds-muted border border-ds-border';
    }
  };

  const radarData = readiness?.perSubject.map(s => ({
    subject: s.name.length > 15 ? s.name.substring(0, 14) + '…' : s.name,
    fullName: s.name,
    readiness: Math.round(s.readiness * 100),
    m: Math.round(s.m * 100),
    a: Math.round(s.a * 100),
    c: Math.round(s.c * 100)
  })) || [];

  return (
    <div className="space-y-8 max-w-7xl mx-auto p-4 md:p-6" data-testid="insights-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-ds-border pb-5">
        <div>
          <div className="flex items-center gap-2 text-ds-accent text-xs font-mono font-semibold uppercase tracking-wider">
            <Activity className="w-4 h-4" />
            <span>GATE CSE 2028 Study Intelligence</span>
          </div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-ds-primary tracking-tight mt-1">
            Progress Analytics & Mastery Insights
          </h1>
          <p className="text-xs text-ds-secondary mt-1">
            Honest syllabus-weighted metrics derived directly from real practice and mock attempts.
          </p>
        </div>

        {/* Exam Countdown Chip */}
        {countdown && (
          <div 
            className="flex items-center gap-3 bg-ds-surface border border-ds-border rounded-ds p-3 shadow-sm self-start sm:self-auto"
            data-testid="countdown-card"
          >
            <div className="w-10 h-10 rounded-ds bg-ds-accent-soft border border-ds-accent/30 flex items-center justify-center text-ds-accent">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <div className="text-[10px] uppercase font-mono text-ds-muted">Target Exam Date</div>
              <div className="font-bold text-sm text-ds-primary font-mono">{countdown.examDate}</div>
              <div className="text-xs text-ds-accent font-semibold font-mono">
                {countdown.daysRemaining} days left ({countdown.weeksRemaining} weeks)
              </div>
            </div>
          </div>
        )}
      </div>

      {loading && (
        <div className="py-20 text-center text-ds-muted text-sm font-mono animate-pulse">
          Computing readiness metrics from real attempts...
        </div>
      )}

      {error && (
        <div className="p-4 rounded-ds bg-ds-danger/10 border border-ds-danger text-ds-danger text-xs font-mono">
          {error}
        </div>
      )}

      {/* Empty State CTA */}
      {isEmpty && (
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-8 text-center space-y-4 max-w-xl mx-auto shadow-sm"
          data-testid="insights-empty-state"
        >
          <div className="w-12 h-12 rounded-full bg-ds-accent-soft text-ds-accent mx-auto flex items-center justify-center">
            <Target className="w-6 h-6" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-ds-primary">No activity yet — take a practice session</h3>
            <p className="text-xs text-ds-secondary">
              Readiness score, subject mastery heatmap, and accuracy trends are derived strictly from completed practice questions and mock tests.
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/practice')}
            className="inline-flex items-center gap-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-5 py-2.5 rounded-ds font-bold text-xs transition-all shadow-sm"
          >
            <span>Start Practicing Now</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {!loading && readiness && (
        <div className="space-y-8">
          {/* Score Hero */}
          <div 
            className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm relative overflow-hidden"
            data-testid="score-hero"
          >
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
              <div className="lg:col-span-4 flex flex-col items-center justify-center text-center p-4 bg-ds-surface-raised rounded-ds border border-ds-border">
                <span className="text-xs font-mono uppercase tracking-wider text-ds-muted">Overall Readiness</span>
                <div className="text-5xl font-black text-ds-accent font-mono my-2 tracking-tight">
                  {readiness.readiness}%
                </div>
                <div className="text-xs text-ds-secondary font-medium">
                  GATE CSE Syllabus-Weighted Aggregate
                </div>

                <button
                  type="button"
                  onClick={() => setShowFormula(!showFormula)}
                  className="mt-3 inline-flex items-center gap-1.5 text-xs text-ds-accent hover:underline font-mono"
                  data-testid="formula-toggle-btn"
                >
                  <HelpCircle className="w-3.5 h-3.5" />
                  <span>How is this computed?</span>
                </button>
              </div>

              {/* Subject Components Breakdown */}
              <div className="lg:col-span-8 space-y-3">
                <div className="flex items-center justify-between text-xs font-mono text-ds-muted pb-1 border-b border-ds-border">
                  <span>Subject Component Breakdown</span>
                  <span>Formula: 0.5·M + 0.3·A + 0.2·C</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-56 overflow-y-auto pr-1">
                  {readiness.perSubject.map(s => (
                    <div 
                      key={s.subjectId}
                      className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border text-xs space-y-1.5"
                    >
                      <div className="flex items-center justify-between font-semibold">
                        <span className="text-ds-primary truncate">{s.name}</span>
                        <span className="font-mono text-ds-accent">{Math.round(s.readiness * 100)}%</span>
                      </div>
                      <div className="flex items-center gap-2 text-[11px] font-mono text-ds-muted">
                        <span title="Mastery">M: {(s.m * 100).toFixed(0)}%</span>
                        <span>·</span>
                        <span title="Accuracy">A: {(s.a * 100).toFixed(0)}%</span>
                        <span>·</span>
                        <span title="Coverage">C: {(s.c * 100).toFixed(0)}%</span>
                        <span>·</span>
                        <span title="Weight">W: {s.weight}%</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Formula Popover / Verbatim Modal */}
            {showFormula && (
              <div 
                className="mt-5 p-4 rounded-ds bg-ds-surface-raised border border-ds-border-strong text-xs space-y-2 animate-fadeIn"
                data-testid="formula-popover"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-ds-primary flex items-center gap-1.5 font-mono">
                    <Info className="w-4 h-4 text-ds-accent" />
                    Binding Formula Specification (§2.2)
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowFormula(false)}
                    className="text-xs text-ds-muted hover:text-ds-primary"
                  >
                    Close
                  </button>
                </div>
                <div className="font-mono bg-ds-bg p-3 rounded-ds border border-ds-border text-ds-secondary space-y-1 text-[11px] overflow-x-auto">
                  <p className="text-ds-accent font-bold">R_s = 0.5·M_s + 0.3·A_s + 0.2·C_s</p>
                  <p>M_s = avg(mastery_level/5) over topics of s with last_revised_at IS NOT NULL</p>
                  <p>A_s = correct / answered on questions of s (attempt_answers with is_correct NOT NULL)</p>
                  <p>C_s = distinct topics of s with ≥1 answered question / total topics of s</p>
                  <p className="text-ds-accent font-bold">READINESS = round(100 · Σ(w_s·R_s) / Σ(w_s))</p>
                  <p className="text-[10px] text-ds-muted pt-1">
                    * Accuracy uses latest distinct-question verdict per question. Inactive subjects count as 0.
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Mastery Heatmap Grid */}
          {heatmap && (
            <div 
              className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm space-y-4"
              data-testid="mastery-heatmap-card"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h2 className="text-base font-bold text-ds-primary flex items-center gap-2">
                    <Flame className="w-4 h-4 text-ds-accent" />
                    <span>Topic Mastery Heatmap</span>
                  </h2>
                  <p className="text-xs text-ds-muted">
                    6-step mastery scale (0–5). Topics highlighted with a yellow ring are due for spaced revision.
                  </p>
                </div>

                {/* Heatmap Legend */}
                <div className="flex items-center gap-1 text-[11px] font-mono text-ds-muted">
                  <span>Scale:</span>
                  {[0, 1, 2, 3, 4, 5].map(lvl => (
                    <div 
                      key={lvl}
                      className={`w-5 h-5 rounded-sm flex items-center justify-center text-[10px] ${getMasteryColorClass(lvl)}`}
                      title={`Mastery Level ${lvl}`}
                    >
                      {lvl}
                    </div>
                  ))}
                  <div className="ml-2 flex items-center gap-1">
                    <div className="w-4 h-4 rounded-sm border-2 border-ds-warning bg-ds-surface-raised" />
                    <span>Due</span>
                  </div>
                </div>
              </div>

              {/* Heatmap Matrix */}
              <div className="space-y-4 max-h-[32rem] overflow-y-auto pr-2">
                {heatmap.subjects.map(subj => (
                  <div key={subj.subjectId} className="space-y-1.5">
                    <div className="text-xs font-bold text-ds-secondary flex items-center justify-between">
                      <span>{subj.name}</span>
                      <span className="text-[11px] font-mono text-ds-muted">{subj.topics.length} topics</span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2">
                      {subj.topics.map(topic => (
                        <div
                          key={topic.topicId}
                          data-testid="heatmap-cell"
                          data-mastery={topic.mastery}
                          data-due={topic.due ? 'true' : 'false'}
                          className={`p-2 rounded-ds text-xs flex flex-col justify-between transition-all group relative cursor-pointer ${
                            getMasteryColorClass(topic.mastery)
                          } ${topic.due ? 'ring-2 ring-ds-warning shadow-sm' : ''}`}
                          onClick={() => navigate(topic.due ? '/mistakes' : `/practice?topic=${topic.topicId}`)}
                          title={`${topic.name} | Mastery: ${topic.mastery}/5 | Answered: ${topic.answered}, Correct: ${topic.correct}${topic.due ? ' | DUE FOR REVISION' : ''}`}
                        >
                          <div className="font-semibold truncate text-[11px]" title={topic.name}>
                            {topic.name}
                          </div>
                          <div className="flex items-center justify-between mt-1 text-[10px] opacity-80 font-mono">
                            <span>Lvl {topic.mastery}</span>
                            {topic.due && <span className="font-bold text-ds-warning">DUE</span>}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Charts Row: Subject Radar + Trends */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Subject Readiness Radar Chart */}
            <div 
              className="lg:col-span-5 bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-3"
              data-testid="radar-chart-card"
            >
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-ds-primary flex items-center gap-2">
                  <Target className="w-4 h-4 text-ds-accent" />
                  <span>Subject Preparedness Radar</span>
                </h3>
                <span className="text-[11px] font-mono text-ds-muted">Readiness %</span>
              </div>

              <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart cx="50%" cy="50%" outerRadius="75%" data={radarData}>
                    <PolarGrid stroke="var(--ds-border)" />
                    <PolarAngleAxis 
                      dataKey="subject" 
                      tick={{ fill: 'var(--ds-text-secondary)', fontSize: 10 }}
                    />
                    <PolarRadiusAxis 
                      angle={30} 
                      domain={[0, 100]} 
                      tick={{ fill: 'var(--ds-text-muted)', fontSize: 9 }}
                    />
                    <Radar 
                      name="Readiness %" 
                      dataKey="readiness" 
                      stroke="var(--ds-accent)" 
                      fill="var(--ds-accent)" 
                      fillOpacity={0.4} 
                    />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Trends: Accuracy & Volume Line Chart */}
            <div 
              className="lg:col-span-7 bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-3"
              data-testid="trends-chart-card"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-sm font-bold text-ds-primary flex items-center gap-2">
                    <TrendingUp className="w-4 h-4 text-ds-accent" />
                    <span>Accuracy & Volume Trends</span>
                  </h3>
                  {trends?.rolling7 && (
                    <div className="text-xs text-ds-secondary font-mono mt-0.5">
                      Rolling 7-day Accuracy: <span className="text-ds-accent font-bold">{trends.rolling7.accuracy}%</span> ({trends.rolling7.volume} answered)
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-1 bg-ds-surface-raised p-1 rounded-ds border border-ds-border text-xs font-mono">
                  <button
                    type="button"
                    onClick={() => setTrendDays(30)}
                    className={`px-2.5 py-1 rounded-ds transition-all ${
                      trendDays === 30 
                        ? 'bg-ds-accent text-ds-accent-contrast font-bold' 
                        : 'text-ds-muted hover:text-ds-primary'
                    }`}
                  >
                    30D
                  </button>
                  <button
                    type="button"
                    onClick={() => setTrendDays(90)}
                    className={`px-2.5 py-1 rounded-ds transition-all ${
                      trendDays === 90 
                        ? 'bg-ds-accent text-ds-accent-contrast font-bold' 
                        : 'text-ds-muted hover:text-ds-primary'
                    }`}
                  >
                    90D
                  </button>
                </div>
              </div>

              {trends?.days && (
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={trends.days} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--ds-border)" />
                      <XAxis 
                        dataKey="date" 
                        tick={{ fill: 'var(--ds-text-muted)', fontSize: 9 }}
                        tickFormatter={(val) => val.slice(5)} 
                      />
                      <YAxis 
                        yAxisId="left"
                        tick={{ fill: 'var(--ds-text-muted)', fontSize: 9 }} 
                      />
                      <Tooltip 
                        contentStyle={{ 
                          backgroundColor: 'var(--ds-surface)', 
                          borderColor: 'var(--ds-border)',
                          borderRadius: '8px',
                          color: 'var(--ds-text-primary)',
                          fontSize: '11px',
                          fontFamily: 'monospace'
                        }} 
                      />
                      <Legend wrapperStyle={{ fontSize: '11px', fontFamily: 'monospace' }} />
                      <Line 
                        yAxisId="left"
                        type="monotone" 
                        dataKey="questionsAnswered" 
                        name="Questions" 
                        stroke="var(--ds-accent)" 
                        dot={false}
                        strokeWidth={2}
                      />
                      <Line 
                        yAxisId="left"
                        type="monotone" 
                        dataKey="correct" 
                        name="Correct" 
                        stroke="var(--ds-success)" 
                        dot={false}
                        strokeWidth={2}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>
          </div>

          {/* Time Investment Stacked Bar Chart */}
          {trends?.days && (
            <div 
              className="bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-3"
              data-testid="minutes-chart-card"
            >
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-ds-primary flex items-center gap-2">
                  <Clock className="w-4 h-4 text-ds-accent" />
                  <span>Time Investment (Practice vs Mock Tests)</span>
                </h3>
                <span className="text-[11px] font-mono text-ds-muted">Minutes per day</span>
              </div>

              <div className="h-56 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={trends.days} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--ds-border)" />
                    <XAxis 
                      dataKey="date" 
                      tick={{ fill: 'var(--ds-text-muted)', fontSize: 9 }}
                      tickFormatter={(val) => val.slice(5)}
                    />
                    <YAxis tick={{ fill: 'var(--ds-text-muted)', fontSize: 9 }} />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: 'var(--ds-surface)', 
                        borderColor: 'var(--ds-border)',
                        borderRadius: '8px',
                        color: 'var(--ds-text-primary)',
                        fontSize: '11px',
                        fontFamily: 'monospace'
                      }} 
                    />
                    <Legend wrapperStyle={{ fontSize: '11px', fontFamily: 'monospace' }} />
                    <Bar dataKey="minutesPracticed" name="Practice (min)" stackId="a" fill="var(--ds-accent)" />
                    <Bar dataKey="minutesMocked" name="Mock (min)" stackId="a" fill="var(--ds-info)" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Achievements & Milestones Strip */}
      <div className="space-y-4 pt-6 border-t border-ds-border" data-testid="achievements-strip">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-base sm:text-lg font-bold text-ds-primary flex items-center gap-2">
              <Trophy className="w-5 h-5 text-ds-accent" />
              <span>Achievements & Milestones</span>
            </h2>
            <p className="text-xs text-ds-muted">
              Dynamically awarded upon reaching genuine milestone events. No fake progress bars.
            </p>
          </div>
          <span className="text-xs font-mono font-semibold px-2.5 py-1 rounded-full bg-ds-surface-raised border border-ds-border text-ds-secondary self-start sm:self-auto">
            {achievements.filter((a) => a.earnedAt).length} / {achievements.length} Unlocked
          </span>
        </div>

        {loadingAchievements ? (
          <div className="py-8 text-center text-xs text-ds-muted font-mono animate-pulse">
            Loading achievement records...
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {achievements.map((item) => {
              const isUnlocked = !!item.earnedAt;
              return (
                <div
                  key={item.code}
                  data-testid={`achievement-card-${item.code}`}
                  className={`p-4 rounded-ds border transition-all flex flex-col justify-between ${
                    isUnlocked
                      ? 'bg-ds-surface border-ds-accent/40 shadow-sm'
                      : 'bg-ds-surface/40 border-ds-border/60 opacity-60'
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <div
                        className={`w-8 h-8 rounded-ds flex items-center justify-center shrink-0 ${
                          isUnlocked
                            ? 'bg-ds-accent-soft text-ds-accent border border-ds-accent/30'
                            : 'bg-ds-surface-raised text-ds-muted border border-ds-border'
                        }`}
                      >
                        {isUnlocked ? (
                          <Trophy className="w-4 h-4" />
                        ) : (
                          <Lock className="w-4 h-4 text-ds-muted" />
                        )}
                      </div>
                      <span
                        data-testid={isUnlocked ? `achievement-unlocked-${item.code}` : `achievement-locked-${item.code}`}
                        className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold ${
                          isUnlocked
                            ? 'bg-ds-success/15 text-ds-success border border-ds-success/30'
                            : 'bg-ds-surface-raised text-ds-muted border border-ds-border'
                        }`}
                      >
                        {isUnlocked ? 'Unlocked' : 'Locked'}
                      </span>
                    </div>

                    <div>
                      <h4
                        className={`text-xs font-bold ${
                          isUnlocked ? 'text-ds-primary' : 'text-ds-secondary'
                        }`}
                      >
                        {item.title}
                      </h4>
                      <p className="text-[11px] text-ds-muted mt-1 leading-snug">
                        {item.description}
                      </p>
                    </div>
                  </div>

                  <div className="pt-3 mt-3 border-t border-ds-border/50 text-[10px] font-mono text-ds-muted">
                    {isUnlocked ? (
                      <span data-testid={`achievement-earned-date-${item.code}`}>
                        Earned {new Date(item.earnedAt!).toLocaleDateString()}
                      </span>
                    ) : (
                      <span>Milestone Locked</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default InsightsPage;
